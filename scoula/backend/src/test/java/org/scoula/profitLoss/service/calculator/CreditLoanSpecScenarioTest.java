package org.scoula.profitLoss.service.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 노션 「신용대출 테스트 케이스(최종)」 시뮬레이션 1~4 검산. ComparisonCalculatorTest(경우 1~8,
// 가입 1·11개월차)와 달리 이 케이스들은 경우 1~6만 다루지만 중도해지이율 구간 tier가 다르다
// (경과 10·11개월 → tier 0.7 근방, 명세서 구간표상 ComparisonCalculatorTest의 1개월차 최저금리
// 분기와는 다른 구간을 지나간다).
class CreditLoanSpecScenarioTest {

    // 명세서는 세후 이자를 원 단위로 먼저 반올림하고, 계산기는 마지막에 한 번만
    // 반올림한다. 그래서 1원 차이가 난다 — assertSavingAmount 가 ±1을 허용한다.
    private static void assertSavingAmount(long expected, long actual) {
        assertTrue(Math.abs(expected - actual) <= 1,
                () -> "savingAmount expected≈" + expected + " but was " + actual);
    }

    private static final Map<Integer, BigDecimal> RATES_BY_PERIOD = Map.of(
            3, new BigDecimal("5.40"),
            6, new BigDecimal("5.71"),
            12, new BigDecimal("6.14")
    );

    private ComparisonCalculator.Result compare(long depositPrincipal, BigDecimal baseRate, BigDecimal maturityRate,
                                                 int contractMonths, int elapsedMonths,
                                                 long urgentAmount, long monthlyPayment,
                                                 boolean isPartialAllowed, boolean isLumpSum) {
        ComparisonCalculator.DepositInput deposit = new ComparisonCalculator.DepositInput(
                depositPrincipal, maturityRate, baseRate, contractMonths, elapsedMonths);
        return ComparisonCalculator.compare(deposit, urgentAmount, monthlyPayment, isPartialAllowed, isLumpSum, RATES_BY_PERIOD);
    }

    // 시뮬레이션 1: 급전 500만 < 예금 1,000만 · 경과 10개월
    // 예금 원금 10,000,000 · 기본 3.0% · 만기 4.0% · 계약 12개월 / 급전 5,000,000 · 월상환 1,000,000
    @Test
    @DisplayName("명세서 시뮬1 · 부분해지O · 만기상환O: LOAN, 절약 54,213원")
    void sim1_partialO_lumpSumO() {
        ComparisonCalculator.Result result = compare(10_000_000L, new BigDecimal("3.0"), new BigDecimal("4.0"),
                12, 10, 5_000_000L, 1_000_000L, true, true);

        assertEquals(ComparisonCalculator.Winner.LOAN, result.winner());
        assertSavingAmount(54_213L, result.savingAmount());
    }

    @Test
    @DisplayName("명세서 시뮬1 · 부분해지X · 만기상환O: LOAN, 절약 152,913원")
    void sim1_partialX_lumpSumO() {
        ComparisonCalculator.Result result = compare(10_000_000L, new BigDecimal("3.0"), new BigDecimal("4.0"),
                12, 10, 5_000_000L, 1_000_000L, false, true);

        assertEquals(ComparisonCalculator.Winner.LOAN, result.winner());
        assertSavingAmount(152_913L, result.savingAmount());
    }

    @Test
    @DisplayName("명세서 시뮬1 · 부분해지O · 만기상환X: LOAN, 절약 23,718원")
    void sim1_partialO_lumpSumX() {
        ComparisonCalculator.Result result = compare(10_000_000L, new BigDecimal("3.0"), new BigDecimal("4.0"),
                12, 10, 5_000_000L, 1_000_000L, true, false);

        assertEquals(ComparisonCalculator.Winner.LOAN, result.winner());
        assertSavingAmount(23_718L, result.savingAmount());
    }

    @Test
    @DisplayName("명세서 시뮬1 · 부분해지X · 만기상환X: LOAN, 절약 122,418원")
    void sim1_partialX_lumpSumX() {
        ComparisonCalculator.Result result = compare(10_000_000L, new BigDecimal("3.0"), new BigDecimal("4.0"),
                12, 10, 5_000_000L, 1_000_000L, false, false);

        assertEquals(ComparisonCalculator.Winner.LOAN, result.winner());
        assertSavingAmount(122_418L, result.savingAmount());
    }

    // 시뮬레이션 2: 급전 500만 < 예금 800만 · 경과 11개월
    // 예금 원금 8,000,000 · 기본 1.5% · 만기 1.5% · 계약 12개월 / 급전 5,000,000 · 월상환 1,000,000
    @Test
    // 명세서는 세후 이자를 원 단위로 먼저 반올림하고, 계산기는 마지막에 한 번만
    // 반올림한다. 그래서 1원 차이가 난다 — assertSavingAmount 가 ±1을 허용한다.
    @DisplayName("명세서 시뮬2 · 부분해지O · 만기상환O: WITHDRAWAL, 절약 11,686원")
    void sim2_partialO_lumpSumO() {
        ComparisonCalculator.Result result = compare(8_000_000L, new BigDecimal("1.5"), new BigDecimal("1.5"),
                12, 11, 5_000_000L, 1_000_000L, true, true);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, result.winner());
        assertSavingAmount(11_686L, result.savingAmount());
    }

    @Test
    // 명세서는 세후 이자를 원 단위로 먼저 반올림하고, 계산기는 마지막에 한 번만
    // 반올림한다. 그래서 1원 차이가 난다 — assertSavingAmount 가 ±1을 허용한다.
    @DisplayName("명세서 시뮬2 · 부분해지X · 만기상환O: WITHDRAWAL, 절약 2,417원")
    void sim2_partialX_lumpSumO() {
        ComparisonCalculator.Result result = compare(8_000_000L, new BigDecimal("1.5"), new BigDecimal("1.5"),
                12, 11, 5_000_000L, 1_000_000L, false, true);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, result.winner());
        assertSavingAmount(2_417L, result.savingAmount());
    }

    @Test
    // 명세서는 세후 이자를 원 단위로 먼저 반올림하고, 계산기는 마지막에 한 번만
    // 반올림한다. 그래서 1원 차이가 난다 — assertSavingAmount 가 ±1을 허용한다.
    @DisplayName("명세서 시뮬2 · 부분해지O · 만기상환X: WITHDRAWAL, 절약 59,536원")
    void sim2_partialO_lumpSumX() {
        ComparisonCalculator.Result result = compare(8_000_000L, new BigDecimal("1.5"), new BigDecimal("1.5"),
                12, 11, 5_000_000L, 1_000_000L, true, false);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, result.winner());
        assertSavingAmount(59_536L, result.savingAmount());
    }

    @Test
    // 명세서는 세후 이자를 원 단위로 먼저 반올림하고, 계산기는 마지막에 한 번만
    // 반올림한다. 그래서 1원 차이가 난다 — assertSavingAmount 가 ±1을 허용한다.
    @DisplayName("명세서 시뮬2 · 부분해지X · 만기상환X: WITHDRAWAL, 절약 50,267원")
    void sim2_partialX_lumpSumX() {
        ComparisonCalculator.Result result = compare(8_000_000L, new BigDecimal("1.5"), new BigDecimal("1.5"),
                12, 11, 5_000_000L, 1_000_000L, false, false);

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, result.winner());
        assertSavingAmount(50_267L, result.savingAmount());
    }

    // 시뮬레이션 3: 급전 = 예금 1,000만 · 경과 10개월
    // 예금 원금 10,000,000 · 기본 3.0% · 만기 4.0% · 계약 12개월 / 급전 10,000,000 · 월상환 1,000,000
    // 급전 = 예금이므로 부분해지 여부는 결과에 영향이 없다 — 두 값을 직접 비교해 확인한다.
    @Test
    @DisplayName("명세서 시뮬3 · 만기상환O: LOAN, 절약 104,012원 (부분해지 O/X 결과 동일)")
    void sim3_lumpSumO_partialInvariant() {
        ComparisonCalculator.Result partialOn = compare(10_000_000L, new BigDecimal("3.0"), new BigDecimal("4.0"),
                12, 10, 10_000_000L, 1_000_000L, true, true);
        ComparisonCalculator.Result partialOff = compare(10_000_000L, new BigDecimal("3.0"), new BigDecimal("4.0"),
                12, 10, 10_000_000L, 1_000_000L, false, true);

        assertEquals(partialOn.winner(), partialOff.winner());
        assertEquals(partialOn.savingAmount(), partialOff.savingAmount());

        assertEquals(ComparisonCalculator.Winner.LOAN, partialOn.winner());
        assertSavingAmount(104_012L, partialOn.savingAmount());
    }

    @Test
    @DisplayName("명세서 시뮬3 · 만기상환X: WITHDRAWAL, 절약 95,101원 (부분해지 O/X 결과 동일)")
    void sim3_lumpSumX_partialInvariant() {
        ComparisonCalculator.Result partialOn = compare(10_000_000L, new BigDecimal("3.0"), new BigDecimal("4.0"),
                12, 10, 10_000_000L, 1_000_000L, true, false);
        ComparisonCalculator.Result partialOff = compare(10_000_000L, new BigDecimal("3.0"), new BigDecimal("4.0"),
                12, 10, 10_000_000L, 1_000_000L, false, false);

        assertEquals(partialOn.winner(), partialOff.winner());
        assertEquals(partialOn.savingAmount(), partialOff.savingAmount());

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, partialOn.winner());
        assertSavingAmount(95_101L, partialOn.savingAmount());
    }

    // 시뮬레이션 4: 급전 = 예금 1,000만 · 경과 11개월
    // 예금 원금 10,000,000 · 기본 1.2% · 만기 1.2% · 계약 12개월 / 급전 10,000,000 · 월상환 2,000,000
    // 급전 = 예금이므로 부분해지 여부는 결과에 영향이 없다 — 두 값을 직접 비교해 확인한다.
    @Test
    // 명세서는 세후 이자를 원 단위로 먼저 반올림하고, 계산기는 마지막에 한 번만
    // 반올림한다. 그래서 1원 차이가 난다 — assertSavingAmount 가 ±1을 허용한다.
    @DisplayName("명세서 시뮬4 · 만기상환O: WITHDRAWAL, 절약 29,519원 (부분해지 O/X 결과 동일)")
    void sim4_lumpSumO_partialInvariant() {
        ComparisonCalculator.Result partialOn = compare(10_000_000L, new BigDecimal("1.2"), new BigDecimal("1.2"),
                12, 11, 10_000_000L, 2_000_000L, true, true);
        ComparisonCalculator.Result partialOff = compare(10_000_000L, new BigDecimal("1.2"), new BigDecimal("1.2"),
                12, 11, 10_000_000L, 2_000_000L, false, true);

        assertEquals(partialOn.winner(), partialOff.winner());
        assertEquals(partialOn.savingAmount(), partialOff.savingAmount());

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, partialOn.winner());
        assertSavingAmount(29_519L, partialOn.savingAmount());
    }

    @Test
    // 명세서는 세후 이자를 원 단위로 먼저 반올림하고, 계산기는 마지막에 한 번만
    // 반올림한다. 그래서 1원 차이가 난다 — assertSavingAmount 가 ±1을 허용한다.
    @DisplayName("명세서 시뮬4 · 만기상환X: WITHDRAWAL, 절약 125,220원 (부분해지 O/X 결과 동일)")
    void sim4_lumpSumX_partialInvariant() {
        ComparisonCalculator.Result partialOn = compare(10_000_000L, new BigDecimal("1.2"), new BigDecimal("1.2"),
                12, 11, 10_000_000L, 2_000_000L, true, false);
        ComparisonCalculator.Result partialOff = compare(10_000_000L, new BigDecimal("1.2"), new BigDecimal("1.2"),
                12, 11, 10_000_000L, 2_000_000L, false, false);

        assertEquals(partialOn.winner(), partialOff.winner());
        assertEquals(partialOn.savingAmount(), partialOff.savingAmount());

        assertEquals(ComparisonCalculator.Winner.WITHDRAWAL, partialOn.winner());
        assertSavingAmount(125_220L, partialOn.savingAmount());
    }
}
