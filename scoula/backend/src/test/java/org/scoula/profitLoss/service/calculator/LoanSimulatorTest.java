package org.scoula.profitLoss.service.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
