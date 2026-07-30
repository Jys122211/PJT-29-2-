package org.scoula.profitLoss.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.profitLoss.dto.ComparisonRequest;
import org.scoula.profitLoss.dto.ComparisonResponse;
import org.scoula.profitLoss.enums.LoanType;
import org.scoula.profitLoss.mapper.ProfitLossMapper;
import org.scoula.profitLoss.service.calculator.ComparisonCalculator;
import org.scoula.profitLoss.vo.LoanProductRateVO;
import org.scoula.profitLoss.vo.UserDepositVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        UserDepositVO deposit = UserDepositVO.builder()
                .userDepositId(10L)
                .userId(userId)
                .productName("KB Star 정기예금")
                .principal(30_000_000L)
                .maturityRate(new BigDecimal("3.2"))
                .baseRate(new BigDecimal("2.4"))
                .contractMonths(12)
                .joinDate(LocalDate.now(SEOUL).minusMonths(1))
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
                UserDepositVO.builder().userDepositId(10L).userId(userId).principal(30_000_000L).build());

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
                UserDepositVO.builder().userDepositId(10L).userId(userId).principal(30_000_000L).build());

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
                UserDepositVO.builder().userDepositId(10L).userId(userId).principal(30_000_000L).build());

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

    // 인터페이스 계약서 4장 EXCEED_LOAN_LIMIT: 조회된 모든 상품의 loanLimit보다 필요금액이 크면 예외.
    @Test
    void compare_throwsExceedLoanLimit_whenUrgentAmountExceedsMaxLoanLimit() {
        Long userId = 1L;

        when(mapper.selectUserDeposit(10L, userId)).thenReturn(UserDepositVO.builder()
                .userDepositId(10L).userId(userId).principal(30_000_000L)
                .maturityRate(new BigDecimal("3.2")).baseRate(new BigDecimal("2.4"))
                .contractMonths(12).joinDate(LocalDate.now(SEOUL).minusMonths(1))
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

        org.junit.jupiter.api.Assertions.assertThrows(ExceedLoanLimitException.class,
                () -> service.compare(userId, request));
    }

    // 한도 이내(정확히 최고한도와 같은 경우 포함)면 예외 없이 정상적으로 계산이 진행돼야 한다.
    @Test
    void compare_succeeds_whenUrgentAmountEqualsMaxLoanLimit() {
        Long userId = 1L;

        when(mapper.selectUserDeposit(10L, userId)).thenReturn(UserDepositVO.builder()
                .userDepositId(10L).userId(userId).principal(30_000_000L)
                .maturityRate(new BigDecimal("3.2")).baseRate(new BigDecimal("2.4"))
                .contractMonths(12).joinDate(LocalDate.now(SEOUL).minusMonths(1))
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
