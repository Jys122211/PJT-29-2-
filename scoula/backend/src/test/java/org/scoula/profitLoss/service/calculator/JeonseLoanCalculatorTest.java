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
        return JeonseLoanCalculator.compare(deposit, URGENT_AMOUNT, MONTHLY_PAYMENT, true, true, RATE_OPTIONS, BigDecimal.ZERO);
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
                deposit, 40_000_000L, 2_000_000L, true, true, RATE_OPTIONS, BigDecimal.ZERO);

        assertEquals(1_097_547L, result.aTotalLoss()); // 해지손실(810,045) + 부족분대출비용(287,502)
        assertEquals(1_928_337L, result.bTotalLoss()); // 원 대출(B안, urgentAmount 전액) — 이번 변경과 무관, 그대로
        assertEquals(JeonseLoanCalculator.Winner.WITHDRAWAL, result.winner());
        assertEquals(830_790L, result.savingAmount());
    }

    // 원 대출(B안)은 urgentAmount 4,000만 전액을 실제기간 11개월(예금잔여)로 갚아야 해 월납입 90만으로는
    // 만기 상환재원이 부족해 불가능하다. 반면 부족분(1,000만) 대출(A안)은 월적립 856,583원으로
    // 실제기간 12개월(≤24, 가능) 만에 완납할 수 있다 — B안 불가·A안 가능이므로 비교 없이 WITHDRAWAL로
    // 확정된다. savingAmount는 0으로 두지 않는다 — comparisons 테이블에는 winner/aFinalBalance/
    // bFinalBalance만 저장되고 savingAmount는 GET 재조회 시 |aFinalBalance-bFinalBalance|로 다시
    // 계산되므로(ProfitLossServiceImpl.buildResponse), 여기서 0으로 강제해도 저장되지 않아 사라진다.
    // winner가 "무엇을 할지"를, savingAmount는 "그 차이가 얼마인지"를 각각 담당한다.
    @Test
    @DisplayName("경우4 · 급전(4,000만) > 예금원금 · 월납입 90만: B안 불가·A안 가능 → WITHDRAWAL, 절약 597,288원")
    void condition4_onlyWithdrawalFeasible() {
        JeonseLoanCalculator.DepositInput deposit = new JeonseLoanCalculator.DepositInput(
                DEPOSIT_PRINCIPAL, MATURITY_RATE, BASE_RATE, CONTRACT_MONTHS, 1);

        JeonseLoanCalculator.Result result = JeonseLoanCalculator.compare(
                deposit, 40_000_000L, 900_000L, true, true, RATE_OPTIONS, BigDecimal.ZERO);

        assertEquals(1_331_049L, result.aTotalLoss()); // 해지손실(810,045) + 부족분대출비용(521,004)
        assertEquals(1_928_337L, result.bTotalLoss()); // 원 대출(B안) 값 자체는 실행 가능 여부와 무관하게 그대로 계산된다
        assertEquals(JeonseLoanCalculator.Winner.WITHDRAWAL, result.winner());
        assertEquals(597_288L, result.savingAmount()); // |1,331,049-1,928,337|
    }

    // 기간 상한을 없앤 뒤에도 "둘 다 불가"는 여전히 존재한다 — 다만 사유가 항상 이자부족이다.
    // 부족분(1,000만)의 월이자는 43,417원이라, 월납입이 그보다 낮으면(여기선 4만원) 월적립≤0이 되어
    // 실제기간 자체를 계산할 수 없다. 원 대출(B안, 4,000만 전액)의 월이자 173,667원도 당연히 못 낸다.
    @Test
    @DisplayName("경우4 · 급전(4,000만) > 예금원금 · 월납입 4만: 둘 다 이자부족으로 불가 → PaymentTooLowException")
    void condition4_bothInfeasible() {
        JeonseLoanCalculator.DepositInput deposit = new JeonseLoanCalculator.DepositInput(
                DEPOSIT_PRINCIPAL, MATURITY_RATE, BASE_RATE, CONTRACT_MONTHS, 1);

        PaymentTooLowException thrown = assertThrows(PaymentTooLowException.class, () ->
                JeonseLoanCalculator.compare(deposit, 40_000_000L, 40_000L, true, true, RATE_OPTIONS, BigDecimal.ZERO));
        assertTrue(thrown.getMessage().contains("이자보다 적어"));
    }

    // isLumpSum=X(비목돈상환) 검산. 예금원금 3,000만/만기3.20%/기본2.40%/계약12개월/경과1개월,
    // 급전 2,000만(<예금, 부분해지O)/최종금리 5.21%. 예금은 만기까지 그대로 유지되므로 A안은
    // isLumpSum=O일 때와 완전히 동일하다(aTotalLoss=540,030, 조건1의 lump-sum 케이스와 같은 값).

    // (가) 월납입 200만: 월이자 86,833 → 월적립 1,913,167 → 실제기간 ceil(2,000만/1,913,167)=11(≤24) →
    //      약정기간 max(12,11)=12 → 대출이자 955,163 + 수수료 9,000 = 964,163.
    //      상환재원(적립금만) 21,044,837 ≥ 2,000만+9,000 → 가능. (실제기간이 예금잔여기간과 우연히
    //      같아 lump-sum 조건1과 수치가 같다 — 이 월납입에서만 그렇다.)
    @Test
    @DisplayName("isLumpSum=X · 월납입 200만: WITHDRAWAL, 절약 424,133원")
    void nonLumpSum_monthlyPayment2m() {
        JeonseLoanCalculator.DepositInput deposit = new JeonseLoanCalculator.DepositInput(
                DEPOSIT_PRINCIPAL, MATURITY_RATE, BASE_RATE, CONTRACT_MONTHS, 1);

        JeonseLoanCalculator.Result result = JeonseLoanCalculator.compare(
                deposit, 20_000_000L, 2_000_000L, true, false, RATE_OPTIONS, BigDecimal.ZERO);

        assertEquals(12, result.loan().commitmentMonths());
        assertEquals(955_163L, result.loan().interest());
        assertEquals(9_000L, result.loan().penalty());
        assertEquals(964_163L, result.bTotalLoss());
        assertEquals(540_030L, result.aTotalLoss()); // isLumpSum과 무관 — A안 해지손실 그대로
        assertEquals(JeonseLoanCalculator.Winner.WITHDRAWAL, result.winner());
        assertEquals(424_133L, result.savingAmount());
    }

    // (나) 월납입 90만: 월이자 86,833 → 월적립 813,167 → 실제기간 ceil(2,000만/813,167)=25.
    //      기간 상한이 없으므로 25개월도 정상적으로 평가된다 — 수수료는 실제=약정이라 0, 상환재원
    //      813,167×25=20,329,175 ≥ 2,000만이라 B안도 실행 가능하다. 둘 다 가능하지만 실제기간이
    //      길어 대출이자(86,833×25=2,170,825)가 크게 쌓여 A안(해지손실 540,030)이 큰 차이로 이긴다.
    @Test
    @DisplayName("isLumpSum=X · 월납입 90만: 실제기간 25개월이라도 둘 다 가능, A안이 큰 차이로 승리")
    void nonLumpSum_monthlyPayment900k_longTermStillFeasible() {
        JeonseLoanCalculator.DepositInput deposit = new JeonseLoanCalculator.DepositInput(
                DEPOSIT_PRINCIPAL, MATURITY_RATE, BASE_RATE, CONTRACT_MONTHS, 1);

        JeonseLoanCalculator.Result result = JeonseLoanCalculator.compare(
                deposit, 20_000_000L, 900_000L, true, false, RATE_OPTIONS, BigDecimal.ZERO);

        assertEquals(540_030L, result.aTotalLoss());
        assertEquals(2_170_825L, result.bTotalLoss());
        assertEquals(JeonseLoanCalculator.Winner.WITHDRAWAL, result.winner());
        assertEquals(1_630_795L, result.savingAmount()); // |540,030-2,170,825|
    }
}
