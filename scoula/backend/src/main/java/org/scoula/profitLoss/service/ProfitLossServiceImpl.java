package org.scoula.profitLoss.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.profitLoss.constant.MinimumWageConstants;
import org.scoula.profitLoss.dto.ComparisonRequest;
import org.scoula.profitLoss.dto.ComparisonResponse;
import org.scoula.profitLoss.enums.LoanType;
import org.scoula.profitLoss.mapper.ProfitLossMapper;
import org.scoula.profitLoss.service.calculator.ComparisonCalculator;
import org.scoula.profitLoss.service.calculator.DepositCalculator;
import org.scoula.profitLoss.vo.ComparisonVO;
import org.scoula.profitLoss.vo.LoanProductRateVO;
import org.scoula.profitLoss.vo.UserDepositVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class ProfitLossServiceImpl implements ProfitLossService {

    private static final ZoneId DEPOSIT_TIMEZONE = ZoneId.of("Asia/Seoul");
    private static final int RATE_CALC_SCALE = 20;
    private static final String MINIMUM_WAGE_WARNING_MESSAGE =
            "이 차액은 최저임금 하루치보다 적어요. 인지세, 보증료, 서류발급비, 교통비 등 부대비용까지 따져보면 실질적인 이득은 적을 수 있어요.";

    private final ProfitLossMapper mapper;

    @Override
    @Transactional
    public ComparisonResponse compare(Long userId, ComparisonRequest request) {
        UserDepositVO deposit = mapper.selectUserDeposit(request.getDeposit().getUserDepositId(), userId);
        if (deposit == null) {
            throw new DepositNotFoundException("예금을 찾을 수 없거나 본인 소유가 아닙니다.");
        }

        // STEP 2 경과월수: join_date ~ 오늘, Asia/Seoul 타임존 기준 (타임존이 어긋나면 경과월수가 밀려
        // 중도해지이율 구간이 바뀐다 — CLAUDE.md에 명시된 함정)
        int elapsedMonths = (int) ChronoUnit.MONTHS.between(deposit.getJoinDate(), LocalDate.now(DEPOSIT_TIMEZONE));

        List<LoanProductRateVO> loanRates = mapper.selectLoanProducts(
                request.getLoan().getLoanProductId(), request.getUserFinancialInfo().getCreditGrade());
        Map<Long, List<LoanProductRateVO>> ratesByProduct = loanRates.stream()
                .collect(Collectors.groupingBy(LoanProductRateVO::getLoanProductId));

        ComparisonCalculator.DepositInput depositInput = new ComparisonCalculator.DepositInput(
                deposit.getPrincipal(), deposit.getMaturityRate(), deposit.getBaseRate(),
                deposit.getContractMonths(), elapsedMonths);

        long urgentAmount = request.getComparisonCondition().getUrgentAmount();
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

        long depositMaturityAmount = deposit.getPrincipal()
                + DepositCalculator.calculateMaturityInterest(deposit.getPrincipal(), deposit.getMaturityRate(), deposit.getContractMonths());

        // "총자산" 관점의 최종 잔액 = 예금을 그대로 뒀을 때의 만기수령액(기준선) − 총손실.
        // A·B에 같은 기준선을 쓰므로 |aFinalBalance − bFinalBalance|는 항상 STEP6의 |A총손실 − B총손실|과 같다.
        long aFinalBalance = depositMaturityAmount - bestResult.aTotalLoss();
        long bFinalBalance = depositMaturityAmount - bestResult.bTotalLoss();

        LocalDateTime now = LocalDateTime.now(DEPOSIT_TIMEZONE);

        ComparisonVO vo = ComparisonVO.builder()
                .userId(userId)
                .userDepositId(deposit.getUserDepositId())
                .urgentAmount(urgentAmount)
                .monthlyPayment(monthlyPayment)
                .isPartialAllowed(isPartialAllowed)
                .isLumpSum(isLumpSum)
                .loanName(bestProduct.getProductName())
                .loanType(bestProduct.getLoanType())
                .loanInterestRate(bestResult.loan().interestRate())
                .ratePeriodMonths(bestResult.loan().ratePeriodMonths())
                .loanInterest(bestResult.loan().interest())
                .loanPenalty(bestResult.loan().penalty())
                .depositName(deposit.getProductName())
                .depositMaintainInterest(bestResult.deposit().maintainInterest())
                .depositCancelInterestRate(bestResult.deposit().cancelInterestRate())
                .depositCancelInterest(bestResult.deposit().cancelInterest())
                .aFinalBalance(aFinalBalance)
                .bFinalBalance(bFinalBalance)
                .winner(bestResult.winner())
                .createdAt(now)
                .build();

        mapper.insertComparison(vo);

        return buildResponse(vo, bestResult);
    }

    // 대출금리(기간) = base(기간) + spread(기간) × 등급배율 − 우대금리(기간, 사용자 적용분)
    // 등급배율 = API_가산(사용자등급) ÷ API_가산(3등급)
    private static BigDecimal resolveFinalRate(LoanProductRateVO rate, BigDecimal totalDiscountRate) {
        BigDecimal gradeMultiplier = rate.getGradeAverageSpreadRate()
                .divide(rate.getBaseGradeAverageSpreadRate(), RATE_CALC_SCALE, RoundingMode.HALF_UP);
        return rate.getBaseRate()
                .add(rate.getSpreadRate().multiply(gradeMultiplier))
                .subtract(totalDiscountRate);
    }

    private ComparisonResponse buildResponse(ComparisonVO vo, ComparisonCalculator.Result result) {
        long netProfit = vo.getDepositMaintainInterest() - result.loan().cost();
        long withdrawalProfit = vo.getDepositMaintainInterest() - result.aTotalLoss();
        boolean isBelowMinimumWage = result.savingAmount() < MinimumWageConstants.DAILY_MINIMUM_WAGE;

        ComparisonResponse.Warning warning = ComparisonResponse.Warning.builder()
                .isBelowMinimumWage(isBelowMinimumWage)
                .minimumWageDaily(MinimumWageConstants.DAILY_MINIMUM_WAGE)
                .message(isBelowMinimumWage ? MINIMUM_WAGE_WARNING_MESSAGE : null)
                .build();

        ComparisonResponse.Badges badges = ComparisonResponse.Badges.builder()
                .recommended(vo.getLoanType() == LoanType.CREDIT ? "신용대출" : "전세자금대출")
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
                .cost(result.loan().cost())
                .isRateEstimated(vo.getLoanType() == LoanType.CREDIT)
                .finalBalance(vo.getBFinalBalance())
                .netProfit(netProfit)
                .build();

        ComparisonResponse.DepositInfo deposit = ComparisonResponse.DepositInfo.builder()
                .name(vo.getDepositName())
                .maintainInterest(vo.getDepositMaintainInterest())
                .cancelInterestRate(vo.getDepositCancelInterestRate())
                .cancelInterest(vo.getDepositCancelInterest())
                .withdrawalProfit(withdrawalProfit)
                .finalBalance(vo.getAFinalBalance())
                .build();

        return ComparisonResponse.builder()
                .comparisonId(vo.getComparisonId())
                .winner(vo.getWinner())
                .savingAmount(result.savingAmount())
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
