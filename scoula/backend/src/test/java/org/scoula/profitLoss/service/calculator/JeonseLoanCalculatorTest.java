package org.scoula.profitLoss.service.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
