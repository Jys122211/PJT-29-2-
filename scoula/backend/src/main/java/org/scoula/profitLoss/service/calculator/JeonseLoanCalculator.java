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

    // 대출 약정기간 하한(상품 조건: 1년 이상). 상한(2년 이내, 최초 약정 기준 — 10년은 재계약 연장을 포함한
    // 값이라 지금 시점 계산에는 쓰지 않는다)은 예금 유지 B안·부족분 A안 양쪽에 동일하게 적용한다.
    private static final int MIN_COMMITMENT_MONTHS = 12;
    private static final int MAX_COMMITMENT_MONTHS = 24;
    private static final BigDecimal PREPAYMENT_FEE_RATE = new BigDecimal("0.0054"); // 중도상환수수료율 0.54% (신용대출 0.11%와 다름)
    private static final BigDecimal MONTHLY_RATE_DIVISOR = new BigDecimal("1200"); // %/100/12
    private static final int CALC_SCALE = 20;

    private static final String INTEREST_TOO_LOW_MESSAGE = "월 상환 가능 금액이 이자보다 적어 대출 상환이 불가능합니다.";
    private static final String TERM_TOO_LONG_MESSAGE =
            "상환 기간이 대출 최장 약정기간(" + MAX_COMMITMENT_MONTHS + "개월)을 초과합니다.";
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

    private JeonseLoanCalculator() {
    }

    public static Result compare(DepositInput depositInput, long urgentAmount, long monthlyPayment,
                                  boolean isPartialAllowed, List<RateOption> rateOptions, BigDecimal totalDiscountRate) {
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

        // STEP 4-2: 실제기간 = 예금잔여기간(예금 만기에 목돈으로 상환), 약정기간 = max(12, 예금잔여기간)
        int actualMonths = depositRemainingMonths;
        int commitmentMonths = Math.max(MIN_COMMITMENT_MONTHS, depositRemainingMonths);

        // STEP 4-3~5: B안(전세대출 전액) 대출비용. 실행 가능 여부와 무관하게 항상 계산 가능한 값이다.
        LoanCostBreakdown mainLoan = computeLoanCost(urgentAmount, finalRate, actualMonths, commitmentMonths);

        // STEP 5: B안 실행 가능성 판정 (기간 상한 → 이자 지불 → 만기 상환 재원). 예외를 던지지 않고 상태만 기록한다.
        LoanFeasibility loanFeasibility = evaluateMainLoanFeasibility(
                urgentAmount, monthlyPayment, mainLoan, actualMonths, commitmentMonths, depositMaturityAmount);

        // STEP 6: A안(예금 깨기) 총손실 — 전세는 만기목돈상환 O 고정이라 4가지 경우뿐이다.
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

        Winner winner;
        long savingAmount;
        if (withdrawalFeasibility.feasible() && loanFeasibility.feasible()) {
            if (aTotalLoss < bTotalLoss) {
                winner = Winner.WITHDRAWAL;
            } else if (aTotalLoss > bTotalLoss) {
                winner = Winner.LOAN;
            } else {
                winner = Winner.TIE;
            }
            savingAmount = Math.abs(aTotalLoss - bTotalLoss);
        } else if (loanFeasibility.feasible()) {
            // A안(예금 해지)이 불가능 — B안만 실행 가능하므로 비교 없이 B안으로 확정.
            winner = Winner.LOAN;
            savingAmount = 0L;
        } else {
            // B안(전세대출 전액)이 불가능 — A안만 실행 가능하므로 비교 없이 A안으로 확정.
            winner = Winner.WITHDRAWAL;
            savingAmount = 0L;
        }

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

    // 월이자 = 급전 × 최종금리 / 12 (원 단위로 먼저 반올림) → 대출이자 = 월이자 × 실제기간.
    // 원금이 줄지 않아 매달 이자가 동일하므로, 명세서 검산값(예: 86,833원×11=955,163원)과 맞추려면
    // 월이자를 먼저 원 단위로 반올림한 뒤 실제기간을 곱해야 한다 — 전체를 소수로 계산한 뒤 한 번에
    // 반올림하면(955,167원) 명세서 값과 어긋난다.
    private static LoanCostBreakdown computeLoanCost(long loanAmount, BigDecimal finalRate, int actualMonths, int commitmentMonths) {
        long monthlyInterest = computeMonthlyInterest(loanAmount, finalRate);
        long interest = monthlyInterest * actualMonths;
        long fee = computeFee(loanAmount, actualMonths, commitmentMonths);

        return new LoanCostBreakdown(monthlyInterest, interest, fee, interest + fee);
    }

    // STEP 5: B안(전세대출 전액) 실행 가능성 — 기간 상한 → 이자 지불 → 만기 상환 재원 순으로 확인한다.
    // 예금잔여기간이 24개월을 넘는 일은 드물지만, 부족분 대출과 같은 기준을 동일하게 적용한다.
    private static LoanFeasibility evaluateMainLoanFeasibility(long urgentAmount, long monthlyPayment, LoanCostBreakdown mainLoan,
                                                                 int actualMonths, int commitmentMonths, long depositMaturityAmount) {
        if (actualMonths > MAX_COMMITMENT_MONTHS || commitmentMonths > MAX_COMMITMENT_MONTHS) {
            return LoanFeasibility.infeasible(TERM_TOO_LONG_MESSAGE);
        }
        if (monthlyPayment < mainLoan.monthlyInterest()) {
            return LoanFeasibility.infeasible(INTEREST_TOO_LOW_MESSAGE);
        }

        long accumulated = (monthlyPayment - mainLoan.monthlyInterest()) * actualMonths;
        long repaymentSource = accumulated + depositMaturityAmount;
        long amountDue = urgentAmount + mainLoan.fee();
        if (repaymentSource < amountDue) {
            return LoanFeasibility.infeasible(INSUFFICIENT_FUNDS_MESSAGE);
        }

        return LoanFeasibility.ok();
    }

    // STEP 6 경우4(급전 > 예금원금)의 부족분 대출. 예금 만기와 무관하게 월납입으로 갚아나가다
    // 적립금(월납입-월이자)이 부족분 원금에 도달하는 시점(ceil)에 완납한다고 본다 — 신용대출이
    // 이 경우 방식1(월납입만으로 완납까지 시뮬레이션)로 처리하는 것과 같은 논리다.
    // 이자조차 못 내면(월적립 ≤ 0) 실제기간을 계산할 수 없고, 계산되더라도 최장 약정기간(24개월)을
    // 넘으면 마찬가지로 실행 불가능하다 — 이자 확인이 먼저다(실제기간 자체가 이자 확인 없이는 정의되지 않는다).
    private static ShortfallLoanEvaluation evaluateShortfallLoan(long shortfall, BigDecimal finalRate, long monthlyPayment) {
        long monthlyInterest = computeMonthlyInterest(shortfall, finalRate);
        long monthlyAccumulation = monthlyPayment - monthlyInterest;
        if (monthlyAccumulation <= 0) {
            return new ShortfallLoanEvaluation(LoanFeasibility.infeasible(INTEREST_TOO_LOW_MESSAGE), null);
        }

        int actualMonths = (int) ceilDiv(shortfall, monthlyAccumulation);
        int commitmentMonths = Math.max(MIN_COMMITMENT_MONTHS, actualMonths);
        if (actualMonths > MAX_COMMITMENT_MONTHS || commitmentMonths > MAX_COMMITMENT_MONTHS) {
            return new ShortfallLoanEvaluation(LoanFeasibility.infeasible(TERM_TOO_LONG_MESSAGE), null);
        }

        long interest = monthlyInterest * actualMonths;
        long fee = computeFee(shortfall, actualMonths, commitmentMonths);
        LoanCostBreakdown breakdown = new LoanCostBreakdown(monthlyInterest, interest, fee, interest + fee);

        return new ShortfallLoanEvaluation(LoanFeasibility.ok(), breakdown);
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
