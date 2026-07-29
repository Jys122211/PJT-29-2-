package org.scoula.profitLoss.service.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoanSimulatorTest {

    @Test
    @DisplayName("방식1: 대출 2,000만원 · 금리 5.71% · 월납입 300만원 → 7개월 완납")
    void 방식1_completesInSevenMonths() {
        LoanSimulator.Result result = LoanSimulator.방식1(20_000_000L, new BigDecimal("5.71"), 3_000_000L);

        assertEquals(7, result.repaymentMonths());
    }

    @Test
    @DisplayName("방식1: 월납입이 첫 달 이자보다 적으면 PaymentTooLowException")
    void 방식1_throwsWhenPaymentTooLow() {
        assertThrows(PaymentTooLowException.class,
                () -> LoanSimulator.방식1(20_000_000L, new BigDecimal("5.71"), 50_000L));
    }

    @Test
    @DisplayName("방식2 조건1 · 가입1개월차: 예금잔여 11개월 만에 목돈으로 완납 → 누적이자 833,166 / 잔액 10,933,166")
    void 방식2_condition1_at1Month_payoffByLumpSum() {
        LoanSimulator.Method2Result result = LoanSimulator.방식2(
                20_000_000L, new BigDecimal("5.71"), 900_000L, 11, 30_812_160L);

        assertEquals(833_166L, result.loanInterest());
        assertEquals(11, result.repaymentMonths());
        assertEquals(10_933_166L, result.lumpSumPrincipal());
        assertFalse(result.convertedToMethod1());
    }

    @Test
    @DisplayName("방식2 조건1 · 가입11개월차: 예금잔여 1개월 만에 목돈으로 완납 → 이자 80,167 / 잔액 19,180,167")
    void 방식2_condition1_at11Months_payoffByLumpSum() {
        LoanSimulator.Method2Result result = LoanSimulator.방식2(
                20_000_000L, new BigDecimal("4.81"), 900_000L, 1, 30_812_160L);

        assertEquals(80_167L, result.loanInterest());
        assertEquals(1, result.repaymentMonths());
        assertEquals(19_180_167L, result.lumpSumPrincipal());
        assertFalse(result.convertedToMethod1());
    }
}
