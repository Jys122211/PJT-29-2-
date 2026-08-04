package org.scoula.profitLoss.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// 전세대출 계산 로직 명세서 STEP 3~6 — 만기일시상환 대출비용과 A·B 총손실 판정.
// DepositCalculator만 재사용한다(예금 계산은 신용대출과 완전히 동일). LoanRateResolver/LoanSimulator/PrepaymentFeeCalculator/
// ComparisonCalculator는 신용대출(rate_period 순환, 원리금균등) 전용이라 여기서는 호출하지 않는다.
// Spring 빈 아님, DB 접근 없음.
public final class JeonseLoanCalculator {

    public enum Winner {
        WITHDRAWAL, LOAN, TIE
    }

    // 대출 약정기간 하한(상품 조건: 1년 이상). 상한은 두지 않는다 — 현실적인 예금금리 범위에서
    // 실제기간이 아무리 길어져도(월적립이 아주 작아도) 대출이자가 그만큼 쌓여 예금 해지가 이기므로,
    // 신용대출(LoanSimulator)이 상한 없이 시뮬레이션하는 것과 같은 이유로 걸러낼 필요가 없다.
    // "월납입 ≥ 월이자" 조건 자체가 실질적인 하한선 역할을 한다.
    private static final int MIN_COMMITMENT_MONTHS = 12;
    private static final BigDecimal PREPAYMENT_FEE_RATE = new BigDecimal("0.0054"); // 중도상환수수료율 0.54% (신용대출 0.11%와 다름)
    private static final BigDecimal MONTHLY_RATE_DIVISOR = new BigDecimal("1200"); // %/100/12
    private static final int CALC_SCALE = 20;

    private static final String INTEREST_TOO_LOW_MESSAGE = "월 상환 가능 금액이 이자보다 적어 대출 상환이 불가능합니다.";
    private static final String INSUFFICIENT_FUNDS_MESSAGE = "만기 상환 재원이 부족해 대출 상환이 불가능합니다.";

    public record DepositInput(long principal, BigDecimal maturityRate, BigDecimal baseRate,
                                int contractMonths, int elapsedMonths) {
    }

    // COFIX 유형(rate_type) 한 옵션의 base_rate/spread_rate. 최종금리 = base+spread-우대, 6개 옵션 중 최솟값.
    public record RateOption(BigDecimal baseRate, BigDecimal spreadRate) {
    }

    public record LoanResult(BigDecimal interestRate, int commitmentMonths, long interest, long penalty, long cost) {
    }

    public record DepositResult(BigDecimal cancelInterestRate, long cancelInterest, long maintainInterest) {
    }

    public record Result(long aTotalLoss, long bTotalLoss, Winner winner, long savingAmount,
                          LoanResult loan, DepositResult deposit) {
    }

    private record LoanCostBreakdown(long monthlyInterest, long interest, long fee, long cost) {
    }

    // A안(예금 해지 [+ 부족분 대출])·B안(전세대출 전액) 각각의 실행 가능 여부. 즉시 예외를 던지지 않고
    // 상태로 들고 있다가, 둘 다 불가능할 때만 compare()에서 최종적으로 예외를 던진다.
    private record LoanFeasibility(boolean feasible, String failureReason) {
        static LoanFeasibility ok() {
            return new LoanFeasibility(true, null);
        }

        static LoanFeasibility infeasible(String reason) {
            return new LoanFeasibility(false, reason);
        }
    }

    private record ShortfallLoanEvaluation(LoanFeasibility feasibility, LoanCostBreakdown breakdown) {
    }

    // B안(전세대출 전액) 판정 결과. commitmentMonths는 실행 가능 여부와 무관하게 LoanResult 표시에 쓰인다.
    private record MainLoanEvaluation(LoanFeasibility feasibility, LoanCostBreakdown breakdown, int commitmentMonths) {
    }

    // 적립식(비목돈상환) 상환의 실제기간·약정기간. 부족분 대출(A안)과 비목돈상환 B안이 공유한다.
    private record ElasticTiming(int actualMonths, int commitmentMonths) {
    }

    private JeonseLoanCalculator() {
    }

    public static Result compare(DepositInput depositInput, long urgentAmount, long monthlyPayment,
                                  boolean isPartialAllowed, boolean isLumpSum,
                                  List<RateOption> rateOptions, BigDecimal totalDiscountRate) {
        // STEP 1: 예금잔여기간 = 계약월수 - 경과월수
        int depositRemainingMonths = depositInput.contractMonths() - depositInput.elapsedMonths();
        // STEP 2-5: 예금만기수령액 = 예금원금 + 만기이자(예금원금)
        long depositMaturityAmount = depositInput.principal()
                + DepositCalculator.calculateMaturityInterest(depositInput.principal(), depositInput.maturityRate(), depositInput.contractMonths());

        BigDecimal cancelInterestRate = DepositCalculator.calculateEarlyWithdrawalRate(
                depositInput.baseRate(), depositInput.elapsedMonths(), depositInput.contractMonths());
        long maintainInterest = DepositCalculator.calculateMaturityInterest(
                depositInput.principal(), depositInput.maturityRate(), depositInput.contractMonths());

        // STEP 3: 최종금리 = 6개 COFIX 옵션 중 (base+spread-우대) 최솟값. 우대 상한은 Service가 이미 적용해서 보낸다.
        BigDecimal finalRate = resolveFinalRate(rateOptions, totalDiscountRate);

        // STEP 4~5: B안(전세대출 전액) 대출비용 + 실행 가능성. 목돈상환 여부로 실제기간 산정 방식이 갈린다.
        MainLoanEvaluation mainLoanEvaluation = isLumpSum
                ? evaluateLumpSumMainLoan(urgentAmount, finalRate, monthlyPayment, depositRemainingMonths, depositMaturityAmount)
                : evaluateNonLumpSumMainLoan(urgentAmount, finalRate, monthlyPayment);
        LoanCostBreakdown mainLoan = mainLoanEvaluation.breakdown();
        LoanFeasibility loanFeasibility = mainLoanEvaluation.feasibility();
        int commitmentMonths = mainLoanEvaluation.commitmentMonths();

        // STEP 6: A안(예금 깨기) 총손실 — 전세는 만기목돈상환 O 고정이라 4가지 경우뿐이다.
        // A안은 isLumpSum과 무관하다(B안의 상환 방식 선택일 뿐, 예금을 깨는 쪽 계산에는 영향이 없다).
        long cancelAmount;
        long aTotalLoss;
        LoanFeasibility withdrawalFeasibility;

        if (urgentAmount < depositInput.principal()) {
            cancelAmount = isPartialAllowed ? urgentAmount : depositInput.principal();
            aTotalLoss = DepositCalculator.calculateWithdrawalLoss(cancelAmount, depositInput.maturityRate(),
                    depositInput.baseRate(), depositInput.elapsedMonths(), depositInput.contractMonths());
            withdrawalFeasibility = LoanFeasibility.ok();
        } else if (urgentAmount == depositInput.principal()) {
            cancelAmount = depositInput.principal();
            aTotalLoss = DepositCalculator.calculateWithdrawalLoss(cancelAmount, depositInput.maturityRate(),
                    depositInput.baseRate(), depositInput.elapsedMonths(), depositInput.contractMonths());
            withdrawalFeasibility = LoanFeasibility.ok();
        } else {
            // 급전 > 예금원금: 예금원금 해지손실 + 부족분 대출비용.
            // 부족분도 월납입으로 갚아나간다 — 매달 이자를 내고 남는 적립금이 부족분 원금에 도달하는
            // 시점에 완납한다고 본다(신용대출 방식1과 같은 논리, 예금 만기와는 무관).
            cancelAmount = depositInput.principal();
            long withdrawalLoss = DepositCalculator.calculateWithdrawalLoss(cancelAmount, depositInput.maturityRate(),
                    depositInput.baseRate(), depositInput.elapsedMonths(), depositInput.contractMonths());
            long shortfall = urgentAmount - depositInput.principal();
            ShortfallLoanEvaluation shortfallEvaluation = evaluateShortfallLoan(shortfall, finalRate, monthlyPayment);
            withdrawalFeasibility = shortfallEvaluation.feasibility();
            // 부족분 대출이 불가능하면 그 비용은 산정할 수 없다(상환 시점이 정의되지 않음) — 해지손실만 반영한다.
            aTotalLoss = withdrawalFeasibility.feasible()
                    ? withdrawalLoss + shortfallEvaluation.breakdown().cost()
                    : withdrawalLoss;
        }

        // 둘 다 불가능할 때만 최종적으로 예외를 던진다.
        if (!withdrawalFeasibility.feasible() && !loanFeasibility.feasible()) {
            throw new PaymentTooLowException(buildBothInfeasibleMessage(
                    withdrawalFeasibility.failureReason(), loanFeasibility.failureReason()));
        }

        long cancelInterest = DepositCalculator.calculateEarlyWithdrawalInterest(cancelAmount, cancelInterestRate, depositInput.elapsedMonths());

        long bTotalLoss = mainLoan.cost();

        // savingAmount는 항상 |A총손실-B총손실|이다 — 한쪽만 가능해도 0으로 두지 않는다.
        // Service 계층(comparisons 21컬럼)은 winner/aFinalBalance/bFinalBalance만 저장하고
        // savingAmount는 GET 재조회 시 |aFinalBalance-bFinalBalance|로 다시 계산한다(ProfitLossServiceImpl
        // buildResponse 참고). 여기서 0으로 강제해도 저장되지 않는 값이라 재조회하면 원래 차액으로
        // 되돌아간다 — "판정은 winner로, 차액 크기는 savingAmount로" 각자의 역할을 분리한다.
        Winner winner;
        if (withdrawalFeasibility.feasible() && loanFeasibility.feasible()) {
            if (aTotalLoss < bTotalLoss) {
                winner = Winner.WITHDRAWAL;
            } else if (aTotalLoss > bTotalLoss) {
                winner = Winner.LOAN;
            } else {
                winner = Winner.TIE;
            }
        } else if (loanFeasibility.feasible()) {
            // A안(예금 해지)이 불가능 — B안만 실행 가능하므로 비교 없이 B안으로 확정.
            winner = Winner.LOAN;
        } else {
            // B안(전세대출 전액)이 불가능 — A안만 실행 가능하므로 비교 없이 A안으로 확정.
            winner = Winner.WITHDRAWAL;
        }
        long savingAmount = Math.abs(aTotalLoss - bTotalLoss);

        LoanResult loanResult = new LoanResult(finalRate, commitmentMonths, mainLoan.interest(), mainLoan.fee(), mainLoan.cost());
        DepositResult depositResult = new DepositResult(cancelInterestRate, cancelInterest, maintainInterest);

        return new Result(aTotalLoss, bTotalLoss, winner, savingAmount, loanResult, depositResult);
    }

    private static BigDecimal resolveFinalRate(List<RateOption> rateOptions, BigDecimal totalDiscountRate) {
        return rateOptions.stream()
                .map(option -> option.baseRate().add(option.spreadRate()).subtract(totalDiscountRate))
                .min(BigDecimal::compareTo)
                .orElseThrow(() -> new IllegalArgumentException("전세대출 금리옵션이 없습니다."));
    }

    // 월이자 = 금액 × 최종금리 / 12 (원 단위로 먼저 반올림) → 이자 = 월이자 × 실제기간.
    // 원금이 줄지 않아 매달 이자가 동일하므로, 명세서 검산값(예: 86,833원×11=955,163원)과 맞추려면
    // 월이자를 먼저 원 단위로 반올림한 뒤 실제기간을 곱해야 한다 — 전체를 소수로 계산한 뒤 한 번에
    // 반올림하면(955,167원) 명세서 값과 어긋난다.
    private static LoanCostBreakdown computeLoanCost(long loanAmount, BigDecimal finalRate, int actualMonths, int commitmentMonths) {
        long monthlyInterest = computeMonthlyInterest(loanAmount, finalRate);
        long interest = monthlyInterest * actualMonths;
        long fee = computeFee(loanAmount, actualMonths, commitmentMonths);

        return new LoanCostBreakdown(monthlyInterest, interest, fee, interest + fee);
    }

    // isLumpSum=O: 실제기간 = 예금잔여기간(예금 만기 목돈 + 적립금으로 상환). 실행 가능성은
    // 이자 지불 → 만기 상환 재원(적립금 + 예금만기수령액) 순으로 확인한다.
    // 실제기간이 예금 조건으로 고정돼 있어, 실행 가능 여부와 무관하게 대출비용은 항상 계산할 수 있다.
    private static MainLoanEvaluation evaluateLumpSumMainLoan(long urgentAmount, BigDecimal finalRate, long monthlyPayment,
                                                                int depositRemainingMonths, long depositMaturityAmount) {
        int actualMonths = depositRemainingMonths;
        int commitmentMonths = Math.max(MIN_COMMITMENT_MONTHS, depositRemainingMonths);
        LoanCostBreakdown breakdown = computeLoanCost(urgentAmount, finalRate, actualMonths, commitmentMonths);

        LoanFeasibility feasibility;
        if (monthlyPayment < breakdown.monthlyInterest()) {
            feasibility = LoanFeasibility.infeasible(INTEREST_TOO_LOW_MESSAGE);
        } else {
            long accumulated = (monthlyPayment - breakdown.monthlyInterest()) * actualMonths;
            long repaymentSource = accumulated + depositMaturityAmount;
            long amountDue = urgentAmount + breakdown.fee();
            feasibility = repaymentSource >= amountDue
                    ? LoanFeasibility.ok()
                    : LoanFeasibility.infeasible(INSUFFICIENT_FUNDS_MESSAGE);
        }

        return new MainLoanEvaluation(feasibility, breakdown, commitmentMonths);
    }

    // isLumpSum=X: 예금은 만기까지 그대로 유지하고, 급전 전액을 적립금(월납입-월이자)만으로 갚는다.
    // 실제기간 = ceil(급전/월적립) — 경우4 부족분 대출(evaluateShortfallLoan)과 동일한 식(computeElasticTiming)을 쓴다.
    // 상환 재원에 예금만기수령액을 더하지 않는다(예금을 쓰지 않으므로).
    private static MainLoanEvaluation evaluateNonLumpSumMainLoan(long urgentAmount, BigDecimal finalRate, long monthlyPayment) {
        long monthlyInterest = computeMonthlyInterest(urgentAmount, finalRate);
        long monthlyAccumulation = monthlyPayment - monthlyInterest;

        if (monthlyAccumulation <= 0) {
            // 실제기간 자체를 정의할 수 없다 — 최소 약정기간(12개월) 기준 참고값만 표시한다.
            LoanCostBreakdown fallback = new LoanCostBreakdown(
                    monthlyInterest, monthlyInterest * MIN_COMMITMENT_MONTHS, 0L, monthlyInterest * MIN_COMMITMENT_MONTHS);
            return new MainLoanEvaluation(LoanFeasibility.infeasible(INTEREST_TOO_LOW_MESSAGE), fallback, MIN_COMMITMENT_MONTHS);
        }

        ElasticTiming timing = computeElasticTiming(urgentAmount, monthlyAccumulation);
        long interest = monthlyInterest * timing.actualMonths();
        long fee = computeFee(urgentAmount, timing.actualMonths(), timing.commitmentMonths());
        LoanCostBreakdown breakdown = new LoanCostBreakdown(monthlyInterest, interest, fee, interest + fee);

        long accumulated = monthlyAccumulation * timing.actualMonths();
        long amountDue = urgentAmount + fee;
        LoanFeasibility feasibility = accumulated >= amountDue
                ? LoanFeasibility.ok()
                : LoanFeasibility.infeasible(INSUFFICIENT_FUNDS_MESSAGE);

        return new MainLoanEvaluation(feasibility, breakdown, timing.commitmentMonths());
    }

    // STEP 6 경우4(급전 > 예금원금)의 부족분 대출. 예금 만기와 무관하게 월납입으로 갚아나가다
    // 적립금(월납입-월이자)이 부족분 원금에 도달하는 시점(ceil)에 완납한다고 본다 — 신용대출이
    // 이 경우 방식1(월납입만으로 완납까지 시뮬레이션)로 처리하는 것과 같은 논리다.
    // 이자조차 못 내면(월적립 ≤ 0) 실제기간을 계산할 수 없어 실행 불가능하다. 기간 자체에는 상한을
    // 두지 않는다 — 실제기간이 길어질수록 이자가 쌓여 자동으로 예금 해지 쪽이 이기므로 걸러낼 필요가 없다.
    private static ShortfallLoanEvaluation evaluateShortfallLoan(long shortfall, BigDecimal finalRate, long monthlyPayment) {
        long monthlyInterest = computeMonthlyInterest(shortfall, finalRate);
        long monthlyAccumulation = monthlyPayment - monthlyInterest;
        if (monthlyAccumulation <= 0) {
            return new ShortfallLoanEvaluation(LoanFeasibility.infeasible(INTEREST_TOO_LOW_MESSAGE), null);
        }

        ElasticTiming timing = computeElasticTiming(shortfall, monthlyAccumulation);
        long interest = monthlyInterest * timing.actualMonths();
        long fee = computeFee(shortfall, timing.actualMonths(), timing.commitmentMonths());
        LoanCostBreakdown breakdown = new LoanCostBreakdown(monthlyInterest, interest, fee, interest + fee);

        return new ShortfallLoanEvaluation(LoanFeasibility.ok(), breakdown);
    }

    // 적립식 상환의 실제기간 = ceil(금액/월적립), 약정기간 = max(12, 실제기간). 부족분 대출(A안)과
    // 비목돈상환 B안이 공유하는 식이다.
    private static ElasticTiming computeElasticTiming(long amount, long monthlyAccumulation) {
        int actualMonths = (int) ceilDiv(amount, monthlyAccumulation);
        int commitmentMonths = Math.max(MIN_COMMITMENT_MONTHS, actualMonths);
        return new ElasticTiming(actualMonths, commitmentMonths);
    }

    private static String buildBothInfeasibleMessage(String withdrawalReason, String loanReason) {
        return "A안(예금 해지)과 B안(전세대출 전액) 모두 실행할 수 없습니다. A안: "
                + withdrawalReason + " / B안: " + loanReason;
    }

    private static long computeMonthlyInterest(long amount, BigDecimal finalRate) {
        return roundToWon(BigDecimal.valueOf(amount)
                .multiply(finalRate)
                .divide(MONTHLY_RATE_DIVISOR, CALC_SCALE, RoundingMode.HALF_UP));
    }

    private static long computeFee(long amount, int actualMonths, int commitmentMonths) {
        BigDecimal feeExact = BigDecimal.valueOf(amount)
                .multiply(PREPAYMENT_FEE_RATE)
                .multiply(BigDecimal.valueOf(commitmentMonths - actualMonths))
                .divide(BigDecimal.valueOf(commitmentMonths), CALC_SCALE, RoundingMode.HALF_UP);
        return roundToWon(feeExact);
    }

    private static long ceilDiv(long numerator, long denominator) {
        return (numerator + denominator - 1) / denominator;
    }

    private static long roundToWon(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
