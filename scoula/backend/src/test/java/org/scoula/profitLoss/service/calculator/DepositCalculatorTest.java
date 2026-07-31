package org.scoula.profitLoss.service.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DepositCalculatorTest {

    private static final long DEPOSIT_AMOUNT = 30_000_000L;
    private static final BigDecimal MATURITY_RATE = new BigDecimal("3.2");
    private static final BigDecimal BASE_RATE = new BigDecimal("2.4");
    private static final int CONTRACT_MONTHS = 12;

    @Test
    @DisplayName("만기이자: 3천만원 · 만기이율 3.2% · 계약 12개월 → 812,160원")
    void calculateMaturityInterest_returns812160() {
        long maturityInterest = DepositCalculator.calculateMaturityInterest(DEPOSIT_AMOUNT, MATURITY_RATE, CONTRACT_MONTHS);

        assertEquals(812_160L, maturityInterest);
    }

    @Test
    @DisplayName("가입 1개월차: 중도해지이율 0.10%, 중도해지이자 2,115원")
    void earlyWithdrawal_at1Month() {
        BigDecimal rate = DepositCalculator.calculateEarlyWithdrawalRate(BASE_RATE, 1, CONTRACT_MONTHS);
        long interest = DepositCalculator.calculateEarlyWithdrawalInterest(DEPOSIT_AMOUNT, rate, 1);

        assertEquals(0, new BigDecimal("0.100").compareTo(rate));
        assertEquals(2_115L, interest);
    }

    @Test
    @DisplayName("가입 11개월차: 중도해지이율 1.98%, 중도해지이자 460,647원")
    void earlyWithdrawal_at11Months() {
        BigDecimal rate = DepositCalculator.calculateEarlyWithdrawalRate(BASE_RATE, 11, CONTRACT_MONTHS);
        long interest = DepositCalculator.calculateEarlyWithdrawalInterest(DEPOSIT_AMOUNT, rate, 11);

        assertEquals(0, new BigDecimal("1.980").compareTo(rate));
        assertEquals(460_647L, interest);
    }
}
