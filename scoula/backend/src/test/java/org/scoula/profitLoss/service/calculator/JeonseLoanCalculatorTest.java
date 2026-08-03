package org.scoula.profitLoss.service.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 전세대출 계산 로직 명세서 「검산 케이스」 조건1·조건2 재현.
class JeonseLoanCalculatorTest {

    // 원 단위 반올림 시점 차이로 ±2원까지 허용한다 (명세서 지시사항).
    private static void assertClose(long expected, long actual) {
        assertTrue(Math.abs(expected - actual) <= 2,
                () -> "expected≈" + expected + " but was " + actual);
    }

    private static final long DEPOSIT_PRINCIPAL = 30_000_000L;
    private static final BigDecimal MATURITY_RATE = new BigDecimal("3.20");
    private static final BigDecimal BASE_RATE = new BigDecimal("2.40");
    private static final int CONTRACT_MONTHS = 12;

    private static final long URGENT_AMOUNT = 20_000_000L;
    private static final long MONTHLY_PAYMENT = 900_000L;

    // COFIX 유형 6종. 최솟값(5.21%)이 선택돼야 한다.
    private static final List<JeonseLoanCalculator.RateOption> RATE_OPTIONS = List.of(
            new JeonseLoanCalculator.RateOption(new BigDecimal("3.05"), new BigDecimal("2.16")), // 5.21 (최소)
            new JeonseLoanCalculator.RateOption(new BigDecimal("2.94"), new BigDecimal("2.34")), // 5.28
            new JeonseLoanCalculator.RateOption(new BigDecimal("2.94"), new BigDecimal("2.34")), // 5.28
            new JeonseLoanCalculator.RateOption(new BigDecimal("3.05"), new BigDecimal("2.27")), // 5.32
            new JeonseLoanCalculator.RateOption(new BigDecimal("2.54"), new BigDecimal("2.96")), // 5.50
            new JeonseLoanCalculator.RateOption(new BigDecimal("2.54"), new BigDecimal("2.99"))  // 5.53
    );

    private JeonseLoanCalculator.Result compare(int elapsedMonths) {
        JeonseLoanCalculator.DepositInput deposit = new JeonseLoanCalculator.DepositInput(
                DEPOSIT_PRINCIPAL, MATURITY_RATE, BASE_RATE, CONTRACT_MONTHS, elapsedMonths);
        return JeonseLoanCalculator.compare(deposit, URGENT_AMOUNT, MONTHLY_PAYMENT, true, RATE_OPTIONS, BigDecimal.ZERO);
    }

    @Test
    @DisplayName("조건1 · 가입1개월차: WITHDRAWAL, 절약 424,133원")
    void condition1_at1Month() {
        JeonseLoanCalculator.Result result = compare(1);

        assertEquals(new BigDecimal("5.21"), result.loan().interestRate());
        assertEquals(955_163L, result.loan().interest());
        assertEquals(9_000L, result.loan().penalty());
        assertEquals(964_163L, result.bTotalLoss());

        assertClose(30_272_130L, DEPOSIT_PRINCIPAL + result.deposit().maintainInterest() - result.aTotalLoss());
        assertClose(29_847_997L, DEPOSIT_PRINCIPAL + result.deposit().maintainInterest() - result.bTotalLoss());

        assertEquals(JeonseLoanCalculator.Winner.WITHDRAWAL, result.winner());
        assertClose(424_133L, result.savingAmount());
    }

    @Test
    @DisplayName("조건2 · 가입11개월차: LOAN, 절약 48,509원")
    void condition2_at11Months() {
        JeonseLoanCalculator.Result result = compare(11);

        assertEquals(new BigDecimal("5.21"), result.loan().interestRate());
        assertEquals(86_833L, result.loan().interest());
        assertEquals(99_000L, result.loan().penalty());
        assertEquals(185_833L, result.bTotalLoss());

        assertClose(30_577_818L, DEPOSIT_PRINCIPAL + result.deposit().maintainInterest() - result.aTotalLoss());
        assertClose(30_626_327L, DEPOSIT_PRINCIPAL + result.deposit().maintainInterest() - result.bTotalLoss());

        assertEquals(JeonseLoanCalculator.Winner.LOAN, result.winner());
        assertClose(48_509L, result.savingAmount());
    }

    // 경우4(급전 > 예금원금): 예금원금 3,000만/만기3.20%/기본2.40%/계약12개월/경과1개월, 급전 4,000만, 최종금리 5.21%.
    // 부족분(1,000만)은 월납입으로 갚아나가다 적립금이 부족분에 도달하는 시점(ceil)에 완납한다고 본다.
    @Test
    @DisplayName("경우4 · 급전(4,000만) > 예금원금 · 월납입 200만: WITHDRAWAL, 절약 830,790원")
    void condition4_shortfallLoan_feasible() {
        JeonseLoanCalculator.DepositInput deposit = new JeonseLoanCalculator.DepositInput(
                DEPOSIT_PRINCIPAL, MATURITY_RATE, BASE_RATE, CONTRACT_MONTHS, 1);

        // 부족분(1,000만) 월이자 43,417원 → 월적립 1,956,583원 → 실제기간 ceil(1,000만/1,956,583)=6개월 →
        // 약정기간 max(12,6)=12 → 대출이자 260,502원 + 수수료 27,000원 = 부족분대출비용 287,502원.
        JeonseLoanCalculator.Result result = JeonseLoanCalculator.compare(
                deposit, 40_000_000L, 2_000_000L, true, RATE_OPTIONS, BigDecimal.ZERO);

        assertEquals(1_097_547L, result.aTotalLoss()); // 해지손실(810,045) + 부족분대출비용(287,502)
        assertEquals(1_928_337L, result.bTotalLoss()); // 원 대출(B안, urgentAmount 전액) — 이번 변경과 무관, 그대로
        assertEquals(JeonseLoanCalculator.Winner.WITHDRAWAL, result.winner());
        assertEquals(830_790L, result.savingAmount());
    }

    // 원 대출(B안)은 urgentAmount 4,000만 전액을 실제기간 11개월(예금잔여)로 갚아야 해 월납입 90만으로는
    // 만기 상환재원이 부족하다 — 부족분 대출 로직과 무관하게 STEP5(B안 전용)에서 먼저 걸린다.
    @Test
    @DisplayName("경우4 · 급전(4,000만) > 예금원금 · 월납입 90만: 원 대출(B안) 상환재원 부족으로 PaymentTooLowException")
    void condition4_mainLoanInfeasible() {
        JeonseLoanCalculator.DepositInput deposit = new JeonseLoanCalculator.DepositInput(
                DEPOSIT_PRINCIPAL, MATURITY_RATE, BASE_RATE, CONTRACT_MONTHS, 1);

        PaymentTooLowException thrown = assertThrows(PaymentTooLowException.class, () ->
                JeonseLoanCalculator.compare(deposit, 40_000_000L, 900_000L, true, RATE_OPTIONS, BigDecimal.ZERO));
        assertTrue(thrown.getMessage().contains("상환 재원"));
    }
}
