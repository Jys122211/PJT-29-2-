package org.scoula.profitLoss.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.profitLoss.domain.UserDepositVO;
import org.scoula.profitLoss.dto.ComparisonRequest;
import org.scoula.profitLoss.dto.ComparisonResponse;
import org.scoula.profitLoss.enums.LoanType;
import org.scoula.profitLoss.mapper.ProfitLossMapper;
import org.scoula.profitLoss.service.calculator.ComparisonCalculator;
import org.scoula.profitLoss.vo.JeonseLoanProductVO;
import org.scoula.profitLoss.vo.LoanProductRateVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfitLossServiceImplTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private ProfitLossMapper mapper;

    @InjectMocks
    private ProfitLossServiceImpl service;

    // 명세서 저자의 반올림 시점을 완벽히 재현하는 건 불가능할 수 있어 ±1원까지 허용한다 (ComparisonCalculatorTest와 동일 정책).
    private static void assertSavingAmount(long expected, long actual) {
        assertTrue(Math.abs(expected - actual) <= 1,
                () -> "savingAmount expected≈" + expected + " but was " + actual);
    }

    // 조건1 · 가입1개월차: 예금 3천만·만기3.2%·기본2.4%·계약12개월(1개월 전 가입) / 급전 2천만 / 부분해지O / 만기목돈상환O / 월납입90만
    @Test
    void compare_condition1_at1Month_returnsWithdrawal() {
        Long userId = 1L;

        LocalDate joinDate = LocalDate.now(SEOUL).minusMonths(1);
        UserDepositVO deposit = UserDepositVO.builder()
                .userDepositId(10L)
                .userId(userId)
                .productName("KB Star 정기예금")
                .principalAmount(30_000_000L)
                .appliedRate(new BigDecimal("3.2"))
                .baseRate(new BigDecimal("2.4"))
                .joinDate(joinDate)
                .maturityDate(joinDate.plusMonths(12))
                .build();
        when(mapper.selectUserDeposit(10L, userId)).thenReturn(deposit);

        // totalDiscountRate=0, spreadRate=0이 되도록 목만들어서 baseRate 자체가 최종 대출금리가 되게 한다
        // — LoanRateResolverTest 등에서 이미 검증된 4.81/5.19/5.71을 그대로 재사용.
        List<LoanProductRateVO> loanRates = List.of(
                loanRate(3, "4.81"), loanRate(6, "5.19"), loanRate(12, "5.71")
        );
        when(mapper.selectLoanProducts(List.of(100L), 3)).thenReturn(loanRates);

        ComparisonRequest request = ComparisonRequest.builder()
                .userFinancialInfo(ComparisonRequest.UserFinancialInfo.builder()
                        .monthlyPayment(900_000L)
                        .creditGrade(3)
                        .build())
                .deposit(ComparisonRequest.DepositCondition.builder()
                        .userDepositId(10L)
                        .isPartialAllowed(true)
                        .build())
                .loan(ComparisonRequest.LoanCondition.builder()
                        .loanProductId(List.of(100L))
                        .loanType(LoanType.CREDIT)
                        .totalDiscountRate(BigDecimal.ZERO)
                        .build())
                .comparisonCondition(ComparisonRequest.ComparisonCondition.builder()
                        .urgentAmount(20_000_000L)
                        .isLumpSum(true)
                        .build())
                .build();

        ComparisonResponse response = service.compare(userId, request);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, response.getWinner());
        assertSavingAmount(299_798L, response.getSavingAmount());
    }

    // GET은 comparisons 21컬럼 + user_deposit 재조회만으로 POST와 같은 파생값을 재현해야 한다 (새로고침 대응).
    @Test
    void getComparison_reconstructsDerivedFieldsFromStoredRow() {
        Long userId = 1L;

        org.scoula.profitLoss.vo.ComparisonVO stored = baseStoredComparison(userId)
                .winner(ComparisonCalculator.Winner.WITHDRAWAL)
                .build();
        when(mapper.selectComparisonById(1L, userId)).thenReturn(stored);
        when(mapper.selectUserDeposit(10L, userId)).thenReturn(
                UserDepositVO.builder().userDepositId(10L).userId(userId).principalAmount(30_000_000L).build());

        ComparisonResponse response = service.getComparison(userId, 1L);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, response.getWinner());
        assertSavingAmount(299_797L, response.getSavingAmount());
        assertEquals(839_827L, response.getLoan().getCost());
        assertEquals(-27_667L, response.getLoan().getNetProfit());
        assertEquals(272_130L, response.getDeposit().getWithdrawalProfit());
        assertEquals("예금 중도해지", response.getBadges().getRecommended());
    }

    // badges.recommended는 API 명세서상 "승자"의 표시명이어야 한다 — 대출 종류(CREDIT/JEONSE)는
    // winner=LOAN일 때만 의미가 있고, WITHDRAWAL/TIE는 각각 고정 문구여야 한다.
    @Test
    void getComparison_recommendsLoanTypeName_whenLoanWins() {
        Long userId = 1L;

        org.scoula.profitLoss.vo.ComparisonVO stored = baseStoredComparison(userId)
                .winner(ComparisonCalculator.Winner.LOAN)
                .loanType(LoanType.CREDIT)
                .build();
        when(mapper.selectComparisonById(1L, userId)).thenReturn(stored);
        when(mapper.selectUserDeposit(10L, userId)).thenReturn(
                UserDepositVO.builder().userDepositId(10L).userId(userId).principalAmount(30_000_000L).build());

        ComparisonResponse response = service.getComparison(userId, 1L);

        assertEquals("신용대출", response.getBadges().getRecommended());
    }

    @Test
    void getComparison_recommendsTieLabel_whenWinnerIsTie() {
        Long userId = 1L;

        org.scoula.profitLoss.vo.ComparisonVO stored = baseStoredComparison(userId)
                .winner(ComparisonCalculator.Winner.TIE)
                .build();
        when(mapper.selectComparisonById(1L, userId)).thenReturn(stored);
        when(mapper.selectUserDeposit(10L, userId)).thenReturn(
                UserDepositVO.builder().userDepositId(10L).userId(userId).principalAmount(30_000_000L).build());

        ComparisonResponse response = service.getComparison(userId, 1L);

        assertEquals("동일", response.getBadges().getRecommended());
    }

    private static org.scoula.profitLoss.vo.ComparisonVO.ComparisonVOBuilder baseStoredComparison(Long userId) {
        return org.scoula.profitLoss.vo.ComparisonVO.builder()
                .comparisonId(1L)
                .userId(userId)
                .userDepositId(10L)
                .urgentAmount(20_000_000L)
                .monthlyPayment(900_000L)
                .isPartialAllowed(true)
                .isLumpSum(true)
                .loanName("KB STAR 신용대출")
                .loanType(LoanType.CREDIT)
                .loanInterestRate(new BigDecimal("5.71"))
                .ratePeriodMonths(12)
                .loanInterest(833_166L)
                .loanPenalty(6_661L)
                .depositName("KB Star 정기예금")
                .depositMaintainInterest(812_160L)
                .depositCancelInterestRate(new BigDecimal("1.980"))
                .depositCancelInterest(307_098L)
                .aFinalBalance(30_272_130L)
                .bFinalBalance(29_972_333L)
                .createdAt(java.time.LocalDateTime.now(SEOUL));
    }

    // 인터페이스 계약서 4장 EXCEED_LOAN_LIMIT: 조회된 모든 상품의 loanLimit보다 필요금액이 크면
    // 예외 대신 feasible=false 응답을 정상(200)으로 내린다 — 비교 불가는 서버 오류가 아니라 계산 결과다.
    @Test
    void compare_returnsInfeasible_whenUrgentAmountExceedsMaxLoanLimit() {
        Long userId = 1L;

        when(mapper.selectUserDeposit(10L, userId)).thenReturn(UserDepositVO.builder()
                .userDepositId(10L).userId(userId).principalAmount(30_000_000L)
                .appliedRate(new BigDecimal("3.2")).baseRate(new BigDecimal("2.4"))
                .joinDate(LocalDate.now(SEOUL).minusMonths(1))
                .maturityDate(LocalDate.now(SEOUL).minusMonths(1).plusMonths(12))
                .build());

        List<LoanProductRateVO> loanRates = List.of(
                loanRate(3, "4.81", 10_000_000L),
                loanRate(6, "5.19", 10_000_000L),
                loanRate(12, "5.71", 10_000_000L)
        );
        when(mapper.selectLoanProducts(List.of(100L), 3)).thenReturn(loanRates);

        ComparisonRequest request = ComparisonRequest.builder()
                .userFinancialInfo(ComparisonRequest.UserFinancialInfo.builder().monthlyPayment(900_000L).creditGrade(3).build())
                .deposit(ComparisonRequest.DepositCondition.builder().userDepositId(10L).isPartialAllowed(true).build())
                .loan(ComparisonRequest.LoanCondition.builder().loanProductId(List.of(100L)).loanType(LoanType.CREDIT).totalDiscountRate(BigDecimal.ZERO).build())
                .comparisonCondition(ComparisonRequest.ComparisonCondition.builder().urgentAmount(20_000_000L).isLumpSum(true).build())
                .build();

        ComparisonResponse response = service.compare(userId, request);

        assertEquals(Boolean.FALSE, response.getFeasible());
        assertEquals("EXCEED_LOAN_LIMIT", response.getReason());
        assertEquals(20_000_000L, response.getUrgentAmount());
        assertEquals(900_000L, response.getMonthlyPayment());
        assertNull(response.getComparisonId());
        assertNull(response.getWinner());
        verify(mapper, never()).insertComparison(any());
    }

    // 한도 이내(정확히 최고한도와 같은 경우 포함)면 예외 없이 정상적으로 계산이 진행돼야 한다.
    @Test
    void compare_succeeds_whenUrgentAmountEqualsMaxLoanLimit() {
        Long userId = 1L;

        when(mapper.selectUserDeposit(10L, userId)).thenReturn(UserDepositVO.builder()
                .userDepositId(10L).userId(userId).principalAmount(30_000_000L)
                .appliedRate(new BigDecimal("3.2")).baseRate(new BigDecimal("2.4"))
                .joinDate(LocalDate.now(SEOUL).minusMonths(1))
                .maturityDate(LocalDate.now(SEOUL).minusMonths(1).plusMonths(12))
                .build());

        List<LoanProductRateVO> loanRates = List.of(
                loanRate(3, "4.81", 20_000_000L),
                loanRate(6, "5.19", 20_000_000L),
                loanRate(12, "5.71", 20_000_000L)
        );
        when(mapper.selectLoanProducts(List.of(100L), 3)).thenReturn(loanRates);

        ComparisonRequest request = ComparisonRequest.builder()
                .userFinancialInfo(ComparisonRequest.UserFinancialInfo.builder().monthlyPayment(900_000L).creditGrade(3).build())
                .deposit(ComparisonRequest.DepositCondition.builder().userDepositId(10L).isPartialAllowed(true).build())
                .loan(ComparisonRequest.LoanCondition.builder().loanProductId(List.of(100L)).loanType(LoanType.CREDIT).totalDiscountRate(BigDecimal.ZERO).build())
                .comparisonCondition(ComparisonRequest.ComparisonCondition.builder().urgentAmount(20_000_000L).isLumpSum(true).build())
                .build();

        ComparisonResponse response = service.compare(userId, request);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, response.getWinner());
        assertSavingAmount(299_798L, response.getSavingAmount());
    }

    // 회귀 방지: buildResponse()는 계산기 Result.savingAmount()를 쓰지 않고 저장된 aFinalBalance/
    // bFinalBalance 둘만으로 savingAmount를 다시 계산한다(GET 재조회 대응 설계). 전세 STEP6 경우4에서
    // B안이 불가능해도 JeonseLoanCalculator가 savingAmount를 0으로 강제하면, 여기서 재계산되는 값과
    // 어긋나 버그가 났었다 — 실제 계산기 단위테스트는 다 통과했는데 API 응답만 틀렸던 사례.
    // 급전(4,000만) > 예금원금(3,000만) · 월납입 90만 → B안 상환재원 부족으로 불가, A안만 가능.
    @Test
    void compare_jeonseCase4_onlyWithdrawalFeasible_savingAmountReflectsRealGap() {
        Long userId = 1L;

        LocalDate joinDate = LocalDate.now(SEOUL).minusMonths(1);
        UserDepositVO deposit = UserDepositVO.builder()
                .userDepositId(10L)
                .userId(userId)
                .productName("KB Star 정기예금")
                .principalAmount(30_000_000L)
                .appliedRate(new BigDecimal("3.2"))
                .baseRate(new BigDecimal("2.4"))
                .joinDate(joinDate)
                .maturityDate(joinDate.plusMonths(12))
                .build();
        when(mapper.selectUserDeposit(10L, userId)).thenReturn(deposit);
        when(mapper.selectJeonseLoanProducts(List.of(200L))).thenReturn(List.of(
                jeonseRate("3.05", "2.16"), // 5.21 (최소)
                jeonseRate("2.94", "2.34"),
                jeonseRate("2.94", "2.34"),
                jeonseRate("3.05", "2.27"),
                jeonseRate("2.54", "2.96"),
                jeonseRate("2.54", "2.99")
        ));

        ComparisonRequest request = ComparisonRequest.builder()
                .userFinancialInfo(ComparisonRequest.UserFinancialInfo.builder()
                        .monthlyPayment(900_000L)
                        .creditGrade(3)
                        .build())
                .deposit(ComparisonRequest.DepositCondition.builder()
                        .userDepositId(10L)
                        .isPartialAllowed(true)
                        .build())
                .loan(ComparisonRequest.LoanCondition.builder()
                        .loanProductId(List.of(200L))
                        .loanType(LoanType.JEONSE)
                        .totalDiscountRate(BigDecimal.ZERO)
                        .build())
                .comparisonCondition(ComparisonRequest.ComparisonCondition.builder()
                        .urgentAmount(40_000_000L)
                        .isLumpSum(true)
                        .build())
                .build();

        ComparisonResponse response = service.compare(userId, request);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, response.getWinner());
        assertEquals(597_288L, response.getSavingAmount());
    }

    private static JeonseLoanProductVO jeonseRate(String baseRate, String spreadRate) {
        return JeonseLoanProductVO.builder()
                .productId(200L)
                .productName("KB 주택전세자금대출")
                .rateType("신규COFIX6개월")
                .baseRate(new BigDecimal(baseRate))
                .spreadRate(new BigDecimal(spreadRate))
                .maxLoanLimit(444_000_000L)
                .build();
    }

    private static LoanProductRateVO loanRate(int ratePeriodMonths, String baseRate) {
        return loanRate(ratePeriodMonths, baseRate, 50_000_000L);
    }

    private static LoanProductRateVO loanRate(int ratePeriodMonths, String baseRate, long loanLimit) {
        return LoanProductRateVO.builder()
                .loanProductId(100L)
                .productName("KB STAR 신용대출")
                .ratePeriodMonths(ratePeriodMonths)
                .baseRate(new BigDecimal(baseRate))
                .spreadRate(BigDecimal.ZERO)
                .loanLimit(loanLimit)
                .build();
    }
}
