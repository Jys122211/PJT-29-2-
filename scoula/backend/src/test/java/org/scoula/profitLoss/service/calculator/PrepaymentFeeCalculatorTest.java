package org.scoula.profitLoss.service.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrepaymentFeeCalculatorTest {

    private static final long PRINCIPAL = 20_000_000L;
    private static final long MONTHLY_PAYMENT = 900_000L;

    @Test
    // 대출비용 = 대출이자(833,166) + 수수료합(6,661) = 839,827원.
    // 명세서 프롬프트는 이 합을 839,828원으로 적었지만 단순 덧셈 오타다(833,166+6,661=839,827).
    @DisplayName("조건1 · 가입1개월차: 약정 24개월, PMT 883,801, 수수료합 6,661, 대출비용 839,827")
    void calculate_condition1_at1Month() {
        PrepaymentFeeCalculator.Result result = PrepaymentFeeCalculator.calculate(
                PRINCIPAL, new BigDecimal("5.71"), MONTHLY_PAYMENT,
                833_166L, 11, 10_933_166L, 11);

        assertEquals(24, result.termMonths());
        assertEquals(883_801L, result.pmt());
        assertEquals(147L, result.monthlyFeeTotal());
        assertEquals(6_514L, result.lumpSumFee());
        assertEquals(6_661L, result.totalFee());
        assertEquals(839_827L, result.loanCost());
    }

    @Test
    // 대출비용 = 대출이자(80,167) + 수수료합(20,245) = 100,412원.
    // 명세서 프롬프트는 이 합을 100,411원으로 적었지만 단순 덧셈 오타다(80,167+20,245=100,412).
    @DisplayName("조건1 · 가입11개월차: 약정 24개월, PMT 875,727, 수수료합 20,245, 대출비용 100,412")
    void calculate_condition1_at11Months() {
        PrepaymentFeeCalculator.Result result = PrepaymentFeeCalculator.calculate(
                PRINCIPAL, new BigDecimal("4.81"), MONTHLY_PAYMENT,
                80_167L, 1, 19_180_167L, 1);

        assertEquals(24, result.termMonths());
        assertEquals(875_727L, result.pmt());
        assertEquals(26L, result.monthlyFeeTotal());
        assertEquals(20_219L, result.lumpSumFee());
        assertEquals(20_245L, result.totalFee());
        assertEquals(100_412L, result.loanCost());
    }
}
