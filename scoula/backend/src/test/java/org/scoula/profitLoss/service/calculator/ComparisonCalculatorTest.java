package org.scoula.profitLoss.service.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComparisonCalculatorTest {

    // 명세서 저자의 반올림 시점을 완벽히 재현하는 건 불가능할 수 있어 절약금액은 ±1원까지 허용한다.
    // 1원 차이는 판정(WITHDRAWAL/LOAN)을 뒤집지 못하므로 승자는 항상 정확히 일치해야 한다.
    private static void assertSavingAmount(long expected, long actual) {
        assertTrue(Math.abs(expected - actual) <= 1,
                () -> "savingAmount expected≈" + expected + " but was " + actual);
    }

    private static final long DEPOSIT_PRINCIPAL = 30_000_000L;
    private static final BigDecimal MATURITY_RATE = new BigDecimal("3.2");
    private static final BigDecimal BASE_RATE = new BigDecimal("2.4");
    private static final int CONTRACT_MONTHS = 12;

    private static final Map<Integer, BigDecimal> RATES_BY_PERIOD = Map.of(
            3, new BigDecimal("4.81"),
            6, new BigDecimal("5.19"),
            12, new BigDecimal("5.71")
    );

    private static final int AT_1_MONTH = 1;
    private static final int AT_11_MONTHS = 11;

    private ComparisonCalculator.Result compare(int elapsedMonths, long urgentAmount, boolean isPartialAllowed,
                                                 boolean isLumpSum, long monthlyPayment) {
        ComparisonCalculator.DepositInput deposit = new ComparisonCalculator.DepositInput(
                DEPOSIT_PRINCIPAL, MATURITY_RATE, BASE_RATE, CONTRACT_MONTHS, elapsedMonths);
        return ComparisonCalculator.compare(deposit, urgentAmount, monthlyPayment, isPartialAllowed, isLumpSum, RATES_BY_PERIOD);
    }

    // 조건 1: 급전 2천만 · 부분해지 O · 만기목돈상환 O · 월납입 90만
    @Test
    @DisplayName("조건1 · 1개월차: WITHDRAWAL, 절약 299,797원")
    void condition1_at1Month() {
        ComparisonCalculator.Result result = compare(AT_1_MONTH, 20_000_000L, true, true, 900_000L);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, result.winner());
        assertSavingAmount(299_797L, result.savingAmount());
    }

    @Test
    // B안 대출비용 = 대출이자(80,167) + 수수료합(20,245) = 100,412원(정확한 합, 명세서 100,411은 오타 — PrepaymentFeeCalculatorTest에서 검증됨).
    // 절약금액 = |234,342 − 100,412| = 133,930원. 명세서 133,931은 위 오타가 그대로 전파된 값이다.
    @DisplayName("조건1 · 11개월차: LOAN, 절약 133,930원")
    void condition1_at11Months() {
        ComparisonCalculator.Result result = compare(AT_11_MONTHS, 20_000_000L, true, true, 900_000L);

        assertEquals(ComparisonCalculator.Winner.LOAN, result.winner());
        assertSavingAmount(133_930L, result.savingAmount());
    }

    // 조건 2: 급전 2천만 · 부분해지 O · 만기목돈상환 X · 월납입 300만
    @Test
    // B안 대출비용 = 대출이자(373,588) + 수수료합(6,290) = 379,878원. 명세서 원본 문서는 379,879원으로 적어 절약금액이 1원 어긋난다.
    @DisplayName("조건2 · 1개월차: LOAN, 절약 160,152원")
    void condition2_at1Month() {
        ComparisonCalculator.Result result = compare(AT_1_MONTH, 20_000_000L, true, false, 3_000_000L);

        assertEquals(ComparisonCalculator.Winner.LOAN, result.winner());
        assertSavingAmount(160_152L, result.savingAmount());
    }

    @Test
    // B안은 방식1이라 경과월수와 무관하게 1개월차와 동일한 379,878원. 절약금액 = |234,342 − 379,878| = 145,536원.
    @DisplayName("조건2 · 11개월차: WITHDRAWAL, 절약 145,536원")
    void condition2_at11Months() {
        ComparisonCalculator.Result result = compare(AT_11_MONTHS, 20_000_000L, true, false, 3_000_000L);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, result.winner());
        assertSavingAmount(145_536L, result.savingAmount());
    }

    // 조건 3: 급전 2천만 · 부분해지 X · 만기목돈상환 O · 월납입 90만
    @Test
    // B안 대출비용 = 839,827원(정확한 합, 명세서 839,828은 오타 — PrepaymentFeeCalculatorTest에서 검증됨). 절약 = |810,045 − 839,827| = 29,782원.
    @DisplayName("조건3 · 1개월차: WITHDRAWAL, 절약 29,782원")
    void condition3_at1Month() {
        ComparisonCalculator.Result result = compare(AT_1_MONTH, 20_000_000L, false, true, 900_000L);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, result.winner());
        assertSavingAmount(29_782L, result.savingAmount());
    }

    @Test
    // B안 대출비용 = 100,412원(정확한 합). 절약 = |351,513 − 100,412| = 251,101원.
    @DisplayName("조건3 · 11개월차: LOAN, 절약 251,101원")
    void condition3_at11Months() {
        ComparisonCalculator.Result result = compare(AT_11_MONTHS, 20_000_000L, false, true, 900_000L);

        assertEquals(ComparisonCalculator.Winner.LOAN, result.winner());
        assertSavingAmount(251_101L, result.savingAmount());
    }

    // 조건 4: 급전 2천만 · 부분해지 X · 만기목돈상환 X · 월납입 200만
    @Test
    @DisplayName("조건4 · 1개월차: LOAN, 절약 267,231원")
    void condition4_at1Month() {
        ComparisonCalculator.Result result = compare(AT_1_MONTH, 20_000_000L, false, false, 2_000_000L);

        assertEquals(ComparisonCalculator.Winner.LOAN, result.winner());
        assertSavingAmount(267_231L, result.savingAmount());
    }

    @Test
    @DisplayName("조건4 · 11개월차: WITHDRAWAL, 절약 191,301원")
    void condition4_at11Months() {
        ComparisonCalculator.Result result = compare(AT_11_MONTHS, 20_000_000L, false, false, 2_000_000L);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, result.winner());
        assertSavingAmount(191_301L, result.savingAmount());
    }

    // 조건 5: 급전 3천만(=예금원금) · 만기목돈상환 O · 월납입 100만
    @Test
    @DisplayName("조건5 · 1개월차: WITHDRAWAL, 절약 547,694원")
    void condition5_at1Month() {
        ComparisonCalculator.Result result = compare(AT_1_MONTH, 30_000_000L, false, true, 1_000_000L);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, result.winner());
        assertSavingAmount(547_694L, result.savingAmount());
    }

    @Test
    @DisplayName("조건5 · 11개월차: LOAN, 절약 200,172원")
    void condition5_at11Months() {
        ComparisonCalculator.Result result = compare(AT_11_MONTHS, 30_000_000L, false, true, 1_000_000L);

        assertEquals(ComparisonCalculator.Winner.LOAN, result.winner());
        assertSavingAmount(200_172L, result.savingAmount());
    }

    // 조건 6: 급전 3천만(=예금원금) · 만기목돈상환 X · 월납입 400만
    @Test
    @DisplayName("조건6 · 1개월차: LOAN, 절약 179,563원")
    void condition6_at1Month() {
        ComparisonCalculator.Result result = compare(AT_1_MONTH, 30_000_000L, false, false, 4_000_000L);

        assertEquals(ComparisonCalculator.Winner.LOAN, result.winner());
        assertSavingAmount(179_563L, result.savingAmount());
    }

    @Test
    @DisplayName("조건6 · 11개월차: WITHDRAWAL, 절약 278,969원")
    void condition6_at11Months() {
        ComparisonCalculator.Result result = compare(AT_11_MONTHS, 30_000_000L, false, false, 4_000_000L);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, result.winner());
        assertSavingAmount(278_969L, result.savingAmount());
    }

    // 조건 7: 급전 4천만(>예금원금) · 만기목돈상환 O · 월납입 200만
    @Test
    @DisplayName("조건7 · 1개월차: WITHDRAWAL, 절약 678,152원")
    void condition7_at1Month() {
        ComparisonCalculator.Result result = compare(AT_1_MONTH, 40_000_000L, false, true, 2_000_000L);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, result.winner());
        assertSavingAmount(678_152L, result.savingAmount());
    }

    @Test
    // 명세서 조건7·11개월차 워크드 예제는 "1개월 유지+목돈"만으로 완납된다고 서술하지만 실제로는 아니다:
    // 대출 4천만 − 1개월치 정상납입 후 잔액(약 3,816만) − 목돈(30,812,160) = 약 730만원이 남아
    // STEP4 방식2의 구간 C(잔액을 월납입으로 계속)가 반드시 필요하다. 명세서가 이 구간C를 빠뜨려
    // 절약금액이 295,426원으로 계산됐고, 구간C까지 반영한 정확한 값은 아래 206,636원이다(승자 LOAN은 동일).
    @DisplayName("조건7 · 11개월차: LOAN, 절약 206,636원 (명세서는 구간C 누락 오류로 295,426원 — 승자는 동일)")
    void condition7_at11Months() {
        ComparisonCalculator.Result result = compare(AT_11_MONTHS, 40_000_000L, false, true, 2_000_000L);

        assertEquals(ComparisonCalculator.Winner.LOAN, result.winner());
        assertSavingAmount(206_636L, result.savingAmount());
    }

    // 조건 8: 급전 4천만(>예금원금) · 만기목돈상환 X · 월납입 1000만
    @Test
    @DisplayName("조건8 · 1개월차: LOAN, 절약 398,477원")
    void condition8_at1Month() {
        ComparisonCalculator.Result result = compare(AT_1_MONTH, 40_000_000L, false, false, 10_000_000L);

        assertEquals(ComparisonCalculator.Winner.LOAN, result.winner());
        assertSavingAmount(398_477L, result.savingAmount());
    }

    @Test
    @DisplayName("조건8 · 11개월차: WITHDRAWAL, 절약 60,055원")
    void condition8_at11Months() {
        ComparisonCalculator.Result result = compare(AT_11_MONTHS, 40_000_000L, false, false, 10_000_000L);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, result.winner());
        assertSavingAmount(60_055L, result.savingAmount());
    }
}
