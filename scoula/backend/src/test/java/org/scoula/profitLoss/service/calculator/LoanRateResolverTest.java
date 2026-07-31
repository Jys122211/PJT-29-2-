package org.scoula.profitLoss.service.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LoanRateResolverTest {

    private static final Map<Integer, BigDecimal> RATES_BY_PERIOD = Map.of(
            3, new BigDecimal("4.81"),
            6, new BigDecimal("5.19"),
            12, new BigDecimal("5.71")
    );

    @Test
    @DisplayName("만기목돈 O · 예금잔여 11개월 → 12개월물 5.71% 확정 (조건1 · 가입1개월차)")
    void resolve_lumpSum_11MonthsRemaining_confirms12MonthPeriod() {
        LoanRateResolver.Resolution resolution = LoanRateResolver.resolve(
                20_000_000L, 900_000L, true, 11, 30_812_160L, RATES_BY_PERIOD);

        assertEquals(12, resolution.ratePeriodMonths());
        assertEquals(0, new BigDecimal("5.71").compareTo(resolution.loanRate()));
        assertEquals(833_166L, resolution.loanInterest());
        assertEquals(11, resolution.repaymentMonths());
        assertEquals(10_933_166L, resolution.lumpSumPrincipal());
        assertFalse(resolution.convertedToMethod1());
    }

    @Test
    @DisplayName("만기목돈 O · 예금잔여 1개월 → 3개월물 4.81% 확정 (조건1 · 가입11개월차)")
    void resolve_lumpSum_1MonthRemaining_confirms3MonthPeriod() {
        LoanRateResolver.Resolution resolution = LoanRateResolver.resolve(
                20_000_000L, 900_000L, true, 1, 30_812_160L, RATES_BY_PERIOD);

        assertEquals(3, resolution.ratePeriodMonths());
        assertEquals(0, new BigDecimal("4.81").compareTo(resolution.loanRate()));
        assertEquals(80_167L, resolution.loanInterest());
        assertEquals(1, resolution.repaymentMonths());
        assertEquals(19_180_167L, resolution.lumpSumPrincipal());
        assertFalse(resolution.convertedToMethod1());
    }

    @Test
    @DisplayName("만기목돈 X · 월 300만 → 임시 7개월 → 12개월물 5.71% 확정, 방식1 7개월 완납")
    void resolve_noLumpSum_confirms12MonthPeriodWithSevenMonthPayoff() {
        LoanRateResolver.Resolution resolution = LoanRateResolver.resolve(
                20_000_000L, 3_000_000L, false, 0, 0L, RATES_BY_PERIOD);

        assertEquals(12, resolution.ratePeriodMonths());
        assertEquals(0, new BigDecimal("5.71").compareTo(resolution.loanRate()));
        assertEquals(7, resolution.repaymentMonths());
        assertEquals(373_588L, resolution.loanInterest());
        assertEquals(0L, resolution.lumpSumPrincipal());
        assertFalse(resolution.convertedToMethod1());
    }
}
