package org.scoula.profitLoss.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.profitLoss.constant.MinimumWageConstants;
import org.scoula.profitLoss.domain.UserDepositVO;
import org.scoula.profitLoss.dto.ComparisonRequest;
import org.scoula.profitLoss.dto.ComparisonResponse;
import org.scoula.profitLoss.dto.ComparisonSummaryDTO;
import org.scoula.profitLoss.dto.UserDepositDTO;
import org.scoula.profitLoss.enums.LoanType;
import org.scoula.profitLoss.mapper.ProfitLossMapper;
import org.scoula.profitLoss.service.calculator.ComparisonCalculator;
import org.scoula.profitLoss.service.calculator.DepositCalculator;
import org.scoula.profitLoss.service.calculator.JeonseLoanCalculator;
import org.scoula.profitLoss.service.calculator.PaymentTooLowException;
import org.scoula.profitLoss.vo.ComparisonVO;
import org.scoula.profitLoss.vo.JeonseLoanProductVO;
import org.scoula.profitLoss.vo.LoanProductRateVO;
import org.scoula.deposit.util.AccountCrypto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class ProfitLossServiceImpl implements ProfitLossService {

    private static final ZoneId DEPOSIT_TIMEZONE = ZoneId.of("Asia/Seoul");
    private static final String MINIMUM_WAGE_WARNING_MESSAGE =
            "이 차액은 최저임금 하루치보다 적어요. 인지세, 보증료, 서류발급비, 교통비 등 부대비용까지 따져보면 실질적인 이득은 적을 수 있어요.";

    private final ProfitLossMapper mapper;

    // ── 입력 화면 (자금 입력 → 자격 확인 → 우대금리 확인)

    @Override
    public List<UserDepositDTO> getDeposits(Long userId) {
        log.info("getDeposits..........userId={}", userId);

        return mapper.getDepositsByUserId(userId).stream()
                .map(UserDepositDTO::of)
                .toList();
    }

    @Override
    public List<Long> getQualifiedLoanProductIds(List<Long> qualificationQuestionIds) {
        List<Long> sanitizedQuestionIds = qualificationQuestionIds == null
                ? List.of()
                : qualificationQuestionIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        log.info(
                "getQualifiedLoanProductIds..........qualificationQuestionIds={}",
                sanitizedQuestionIds
        );

        return mapper.selectQualifiedLoanProductIds(
                sanitizedQuestionIds,
                sanitizedQuestionIds.size()
        );
    }

    @Override
    public BigDecimal getFinalDiscountRate(
            Long loanProductId,
            List<Long> preferentialQuestionIds
    ) {
        if (loanProductId == null) {
            throw new IllegalArgumentException("loanProductId는 필수입니다.");
        }

        List<Long> sanitizedQuestionIds = preferentialQuestionIds == null
                ? List.of()
                : preferentialQuestionIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        log.info(
                "getFinalDiscountRate..........loanProductId={}, preferentialQuestionIds={}",
                loanProductId,
                sanitizedQuestionIds
        );

        return Optional.ofNullable(
                mapper.selectFinalDiscountRate(
                        loanProductId,
                        sanitizedQuestionIds
                )
        ).orElse(BigDecimal.ZERO);
    }

    // ── 득실 비교

    @Override
    @Transactional
    public ComparisonResponse compare(Long userId, ComparisonRequest request) {
        UserDepositVO deposit = mapper.selectUserDeposit(request.getDeposit().getUserDepositId(), userId);
        if (deposit == null) {
            throw new DepositNotFoundException("예금을 찾을 수 없거나 본인 소유가 아닙니다.");
        }

        // domain/UserDepositVO에는 contractMonths 컬럼이 없어 join_date~maturity_date로 파생시킨다.
        int contractMonths = (int) ChronoUnit.MONTHS.between(deposit.getJoinDate(), deposit.getMaturityDate());

        // STEP 2 경과월수: join_date ~ 오늘, Asia/Seoul 타임존 기준 (타임존이 어긋나면 경과월수가 밀려
        // 중도해지이율 구간이 바뀐다 — CLAUDE.md에 명시된 함정)
        int elapsedMonths = (int) ChronoUnit.MONTHS.between(deposit.getJoinDate(), LocalDate.now(DEPOSIT_TIMEZONE));

        try {
            ComparisonVO vo = request.getLoan().getLoanType() == LoanType.JEONSE
                    ? compareJeonse(userId, request, deposit, contractMonths, elapsedMonths)
                    : compareCredit(userId, request, deposit, contractMonths, elapsedMonths);

            mapper.insertComparison(vo);

            return buildResponse(vo, deposit.getPrincipalAmount());
        } catch (PaymentTooLowException | ExceedLoanLimitException e) {
            // 비교 불가는 서버 오류가 아니라 계산 결과다 — 이력에 남길 게 없으니 저장하지 않고 200으로 내린다.
            return buildInfeasibleResponse(request, e);
        }
    }

    private static ComparisonResponse buildInfeasibleResponse(ComparisonRequest request, RuntimeException e) {
        String reason = e instanceof PaymentTooLowException ? "PAYMENT_TOO_LOW" : "EXCEED_LOAN_LIMIT";

        return ComparisonResponse.builder()
                .feasible(false)
                .reason(reason)
                .urgentAmount(request.getComparisonCondition().getUrgentAmount())
                .monthlyPayment(request.getUserFinancialInfo().getMonthlyPayment())
                .build();
    }

    // loanType=CREDIT 경로. rate_period(3/6/12) 순환 해결 + 원리금균등 상환 시뮬레이션.
    private ComparisonVO compareCredit(Long userId, ComparisonRequest request, UserDepositVO deposit,
                                        int contractMonths, int elapsedMonths) {
        List<LoanProductRateVO> loanRates = mapper.selectLoanProducts(
                request.getLoan().getLoanProductId(), request.getUserFinancialInfo().getCreditGrade());

        long urgentAmount = request.getComparisonCondition().getUrgentAmount();

        // 인터페이스 계약서 4장 EXCEED_LOAN_LIMIT: 여러 상품을 비교하는 구조라 하나라도 한도를 만족하면 통과시킨다.
        long maxLoanLimit = loanRates.stream().mapToLong(LoanProductRateVO::getLoanLimit).max().orElse(0L);
        if (urgentAmount > maxLoanLimit) {
            throw new ExceedLoanLimitException(String.format(
                    "필요금액이 대출 최고한도를 초과합니다. (필요 %d원 / 한도 %d원)", urgentAmount, maxLoanLimit));
        }

        Map<Long, List<LoanProductRateVO>> ratesByProduct = loanRates.stream()
                .collect(Collectors.groupingBy(LoanProductRateVO::getLoanProductId));

        ComparisonCalculator.DepositInput depositInput = new ComparisonCalculator.DepositInput(
                deposit.getPrincipalAmount(), deposit.getAppliedRate(), deposit.getBaseRate(),
                contractMonths, elapsedMonths);

        long monthlyPayment = request.getUserFinancialInfo().getMonthlyPayment();
        boolean isPartialAllowed = Boolean.TRUE.equals(request.getDeposit().getIsPartialAllowed());
        boolean isLumpSum = Boolean.TRUE.equals(request.getComparisonCondition().getIsLumpSum());
        BigDecimal totalDiscountRate = request.getLoan().getTotalDiscountRate() == null
                ? BigDecimal.ZERO : request.getLoan().getTotalDiscountRate();

        ComparisonCalculator.Result bestResult = null;
        LoanProductRateVO bestProduct = null;

        for (Map.Entry<Long, List<LoanProductRateVO>> entry : ratesByProduct.entrySet()) {
            Map<Integer, BigDecimal> ratesByPeriod = entry.getValue().stream()
                    .collect(Collectors.toMap(LoanProductRateVO::getRatePeriodMonths,
                            rate -> resolveFinalRate(rate, totalDiscountRate)));

            ComparisonCalculator.Result result = ComparisonCalculator.compare(
                    depositInput, urgentAmount, monthlyPayment, isPartialAllowed, isLumpSum, ratesByPeriod);

            // b_final_balance(총자산)가 최대인 상품을 고른다 = 대출비용(bTotalLoss)이 가장 작은 상품을 고른다.
            if (bestResult == null || result.bTotalLoss() < bestResult.bTotalLoss()) {
                bestResult = result;
                bestProduct = entry.getValue().get(0);
            }
        }

        if (bestResult == null) {
            throw new IllegalArgumentException("비교할 대출 상품이 없습니다.");
        }

        long depositMaturityAmount = deposit.getPrincipalAmount()
                + DepositCalculator.calculateMaturityInterest(deposit.getPrincipalAmount(), deposit.getAppliedRate(), contractMonths);

        // "총자산" 관점의 최종 잔액 = 예금을 그대로 뒀을 때의 만기수령액(기준선) − 총손실.
        // A·B에 같은 기준선을 쓰므로 |aFinalBalance − bFinalBalance|는 항상 STEP6의 |A총손실 − B총손실|과 같다.
        long aFinalBalance = depositMaturityAmount - bestResult.aTotalLoss();
        long bFinalBalance = depositMaturityAmount - bestResult.bTotalLoss();

        return ComparisonVO.builder()
                .userId(userId)
                .userDepositId(deposit.getUserDepositId())
                .urgentAmount(urgentAmount)
                .monthlyPayment(monthlyPayment)
                .isPartialAllowed(isPartialAllowed)
                .isLumpSum(isLumpSum)
                .loanName(bestProduct.getProductName())
                .loanType(request.getLoan().getLoanType())
                .loanInterestRate(bestResult.loan().interestRate())
                .ratePeriodMonths(bestResult.loan().ratePeriodMonths())
                .loanInterest(bestResult.loan().interest())
                .loanPenalty(bestResult.loan().penalty())
                .depositName(deposit.getProductName())
                .depositAccountNumber(AccountCrypto.decrypt(deposit.getAccountNumber()))
                .depositMaintainInterest(bestResult.deposit().maintainInterest())
                .depositCancelInterestRate(bestResult.deposit().cancelInterestRate())
                .depositCancelInterest(bestResult.deposit().cancelInterest())
                .aFinalBalance(aFinalBalance)
                .bFinalBalance(bFinalBalance)
                .winner(bestResult.winner())
                .createdAt(LocalDateTime.now(DEPOSIT_TIMEZONE))
                .build();
    }

    // loanType=JEONSE 경로. rate_period 순환 없음(COFIX 유형 6종 중 최저), 만기일시상환.
    private ComparisonVO compareJeonse(Long userId, ComparisonRequest request, UserDepositVO deposit,
                                        int contractMonths, int elapsedMonths) {
        List<JeonseLoanProductVO> jeonseRates = mapper.selectJeonseLoanProducts(request.getLoan().getLoanProductId());

        long urgentAmount = request.getComparisonCondition().getUrgentAmount();

        long maxLoanLimit = jeonseRates.stream().mapToLong(JeonseLoanProductVO::getMaxLoanLimit).max().orElse(0L);
        if (urgentAmount > maxLoanLimit) {
            throw new ExceedLoanLimitException(String.format(
                    "필요금액이 대출 최고한도를 초과합니다. (필요 %d원 / 한도 %d원)", urgentAmount, maxLoanLimit));
        }

        Map<Long, List<JeonseLoanProductVO>> ratesByProduct = jeonseRates.stream()
                .collect(Collectors.groupingBy(JeonseLoanProductVO::getProductId));

        JeonseLoanCalculator.DepositInput depositInput = new JeonseLoanCalculator.DepositInput(
                deposit.getPrincipalAmount(), deposit.getAppliedRate(), deposit.getBaseRate(),
                contractMonths, elapsedMonths);

        long monthlyPayment = request.getUserFinancialInfo().getMonthlyPayment();
        boolean isPartialAllowed = Boolean.TRUE.equals(request.getDeposit().getIsPartialAllowed());
        boolean isLumpSum = Boolean.TRUE.equals(request.getComparisonCondition().getIsLumpSum());
        BigDecimal totalDiscountRate = request.getLoan().getTotalDiscountRate() == null
                ? BigDecimal.ZERO : request.getLoan().getTotalDiscountRate();

        JeonseLoanCalculator.Result bestResult = null;
        JeonseLoanProductVO bestProduct = null;

        for (Map.Entry<Long, List<JeonseLoanProductVO>> entry : ratesByProduct.entrySet()) {
            List<JeonseLoanCalculator.RateOption> rateOptions = entry.getValue().stream()
                    .map(rate -> new JeonseLoanCalculator.RateOption(rate.getBaseRate(), rate.getSpreadRate()))
                    .toList();

            JeonseLoanCalculator.Result result = JeonseLoanCalculator.compare(
                    depositInput, urgentAmount, monthlyPayment, isPartialAllowed, isLumpSum, rateOptions, totalDiscountRate);

            if (bestResult == null || result.bTotalLoss() < bestResult.bTotalLoss()) {
                bestResult = result;
                bestProduct = entry.getValue().get(0);
            }
        }

        if (bestResult == null) {
            throw new IllegalArgumentException("비교할 대출 상품이 없습니다.");
        }

        long depositMaturityAmount = deposit.getPrincipalAmount()
                + DepositCalculator.calculateMaturityInterest(deposit.getPrincipalAmount(), deposit.getAppliedRate(), contractMonths);

        long aFinalBalance = depositMaturityAmount - bestResult.aTotalLoss();
        long bFinalBalance = depositMaturityAmount - bestResult.bTotalLoss();

        return ComparisonVO.builder()
                .userId(userId)
                .userDepositId(deposit.getUserDepositId())
                .urgentAmount(urgentAmount)
                .monthlyPayment(monthlyPayment)
                .isPartialAllowed(isPartialAllowed)
                .isLumpSum(isLumpSum)
                .loanName(bestProduct.getProductName())
                .loanType(LoanType.JEONSE)
                .loanInterestRate(bestResult.loan().interestRate())
                // 전세는 rate_period 개념이 없다(comparisons.rate_period_months는 NOT NULL이라 null 저장 불가).
                // 가장 가까운 의미인 약정기간(max(12, 예금잔여기간))을 대신 저장한다.
                .ratePeriodMonths(bestResult.loan().commitmentMonths())
                .loanInterest(bestResult.loan().interest())
                .loanPenalty(bestResult.loan().penalty())
                .depositName(deposit.getProductName())
                .depositAccountNumber(AccountCrypto.decrypt(deposit.getAccountNumber()))
                .depositMaintainInterest(bestResult.deposit().maintainInterest())
                .depositCancelInterestRate(bestResult.deposit().cancelInterestRate())
                .depositCancelInterest(bestResult.deposit().cancelInterest())
                .aFinalBalance(aFinalBalance)
                .bFinalBalance(bFinalBalance)
                .winner(mapWinner(bestResult.winner()))
                .createdAt(LocalDateTime.now(DEPOSIT_TIMEZONE))
                .build();
    }

    private static ComparisonCalculator.Winner mapWinner(JeonseLoanCalculator.Winner winner) {
        return switch (winner) {
            case WITHDRAWAL -> ComparisonCalculator.Winner.WITHDRAWAL;
            case LOAN -> ComparisonCalculator.Winner.LOAN;
            case TIE -> ComparisonCalculator.Winner.TIE;
        };
    }

    @Override
    public ComparisonResponse getComparison(Long userId, Long comparisonId) {
        ComparisonVO vo = mapper.selectComparisonById(comparisonId, userId);
        if (vo == null) {
            throw new ComparisonNotFoundException("이력을 찾을 수 없거나 본인 소유가 아닙니다.");
        }

        // withdrawalProfit(= aFinalBalance − 예금원금) 계산에 원금이 필요한데 comparisons 21컬럼에는
        // 원금이 없다 — 인터페이스 계약서 5-4절이 명시한 대로 user_deposit을 다시 조회한다.
        UserDepositVO deposit = mapper.selectUserDeposit(vo.getUserDepositId(), userId);
        if (deposit == null) {
            throw new DepositNotFoundException("예금을 찾을 수 없거나 본인 소유가 아닙니다.");
        }

        return buildResponse(vo, deposit.getPrincipalAmount());
    }

    // 히스토리
    @Override
    public List<ComparisonSummaryDTO> getComparisons(Long userId) {
        return mapper.selectComparisonsByUserId(userId).stream()
                .map(vo -> ComparisonSummaryDTO.builder()
                        .comparisonId(vo.getComparisonId())
                        .createdAt(vo.getCreatedAt())
                        .urgentAmount(vo.getUrgentAmount())
                        .recommended(resolveRecommendedLabel(vo))
                        .savingAmount(Math.abs(vo.getAFinalBalance() - vo.getBFinalBalance()))
                        .depositName(vo.getDepositName())          // 추가
                        .loanTypeLabel(loanTypeLabel(vo))          // 추가
                        .accountNumber(AccountCrypto.decrypt(vo.getDepositAccountNumber()))
                        .build())
                .collect(Collectors.toList());
    }
    // 추가
    private static String loanTypeLabel(ComparisonVO vo) {
        return vo.getLoanType() == LoanType.CREDIT ? "신용대출" : "전세자금대출";
    }

    // 대출금리(기간) = base_rate(기간·등급) + spread_rate(기간·등급) − 우대금리(기간, 사용자 적용분)
    // credit_loan_grade_rate가 등급별 행을 직접 갖고 있어 등급배율 계산이 필요 없다.
    private static BigDecimal resolveFinalRate(LoanProductRateVO rate, BigDecimal totalDiscountRate) {
        return rate.getBaseRate()
                .add(rate.getSpreadRate())
                .subtract(totalDiscountRate);
    }

    // badges.recommended = "승자"의 표시명 (API 명세서). 대출 종류(CREDIT/JEONSE) 이름은
    // winner=LOAN일 때만 의미가 있다.
    private static String resolveRecommendedLabel(ComparisonVO vo) {
        switch (vo.getWinner()) {
            case LOAN:
                return vo.getLoanType() == LoanType.CREDIT ? "신용대출" : "전세자금대출";
            case WITHDRAWAL:
                return "예금 중도해지";
            default: // TIE
                return "동일";
        }
    }

    // POST(방금 계산한 값) / GET(저장된 값 재조회) 양쪽 모두 comparisons 21컬럼 + 예금원금만으로
    // 모든 파생값을 재계산할 수 있다 — savingAmount/cost/netProfit/withdrawalProfit 어느 것도
    // 계산기 Result 객체가 따로 필요하지 않다 (아래 각 식 참고).
    private ComparisonResponse buildResponse(ComparisonVO vo, long depositPrincipal) {
        long cost = vo.getLoanInterest() + vo.getLoanPenalty();
        long netProfit = vo.getDepositMaintainInterest() - cost;
        long withdrawalProfit = vo.getAFinalBalance() - depositPrincipal;
        long savingAmount = Math.abs(vo.getAFinalBalance() - vo.getBFinalBalance());
        boolean isBelowMinimumWage = vo.getWinner() == ComparisonCalculator.Winner.LOAN
                && savingAmount < MinimumWageConstants.DAILY_MINIMUM_WAGE;

        ComparisonResponse.Warning warning = ComparisonResponse.Warning.builder()
                .isBelowMinimumWage(isBelowMinimumWage)
                .minimumWageDaily(MinimumWageConstants.DAILY_MINIMUM_WAGE)
                .message(isBelowMinimumWage ? MINIMUM_WAGE_WARNING_MESSAGE : null)
                .build();

        ComparisonResponse.Badges badges = ComparisonResponse.Badges.builder()
                .recommended(resolveRecommendedLabel(vo))
                .isPartialAllowed(vo.getIsPartialAllowed())
                .isLumpSum(vo.getIsLumpSum())
                .build();

        ComparisonResponse.LoanInfo loan = ComparisonResponse.LoanInfo.builder()
                .name(vo.getLoanName())
                .type(vo.getLoanType())
                .interestRate(vo.getLoanInterestRate())
                .ratePeriodMonths(vo.getRatePeriodMonths())
                .interest(vo.getLoanInterest())
                .penalty(vo.getLoanPenalty())
                .cost(cost)
                .isRateEstimated(vo.getLoanType() == LoanType.CREDIT)
                .finalBalance(vo.getBFinalBalance())
                .netProfit(netProfit)
                .build();

        ComparisonResponse.DepositInfo deposit = ComparisonResponse.DepositInfo.builder()
                .name(vo.getDepositName())
                .accountNumber(AccountCrypto.decrypt(vo.getDepositAccountNumber()))
                .maintainInterest(vo.getDepositMaintainInterest())
                .cancelInterestRate(vo.getDepositCancelInterestRate())
                .cancelInterest(vo.getDepositCancelInterest())
                .withdrawalProfit(withdrawalProfit)
                .finalBalance(vo.getAFinalBalance())
                .build();

        return ComparisonResponse.builder()
                .feasible(true)
                .reason(null)
                .comparisonId(vo.getComparisonId())
                .winner(vo.getWinner())
                .savingAmount(savingAmount)
                .urgentAmount(vo.getUrgentAmount())
                .monthlyPayment(vo.getMonthlyPayment())
                .createdAt(vo.getCreatedAt())
                .badges(badges)
                .warning(warning)
                .loan(loan)
                .deposit(deposit)
                .build();
    }
}
