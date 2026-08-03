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

    // 대출 약정기간 최소 조건(상품 조건: 1년 이상). 예금과 무관하게 발생하는 부족분 대출에도 그대로 쓰인다.
    private static final int MIN_COMMITMENT_MONTHS = 12;
    private static final BigDecimal PREPAYMENT_FEE_RATE = new BigDecimal("0.0054"); // 중도상환수수료율 0.54% (신용대출 0.11%와 다름)
    private static final BigDecimal MONTHLY_RATE_DIVISOR = new BigDecimal("1200"); // %/100/12
    private static final int CALC_SCALE = 20;

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

        // STEP 4-3~5: B안(전세대출 전액) 대출비용
        LoanCostBreakdown mainLoan = computeLoanCost(urgentAmount, finalRate, actualMonths, commitmentMonths);

        // STEP 5: 실행 가능성 판정 (이자 지불 → 만기 상환 재원)
        validateFeasibility(urgentAmount, monthlyPayment, mainLoan, actualMonths, depositMaturityAmount);

        // STEP 6: A안(예금 깨기) 총손실 — 전세는 만기목돈상환 O 고정이라 4가지 경우뿐이다.
        long cancelAmount;
        long aTotalLoss;
        if (urgentAmount < depositInput.principal()) {
            cancelAmount = isPartialAllowed ? urgentAmount : depositInput.principal();
            aTotalLoss = DepositCalculator.calculateWithdrawalLoss(cancelAmount, depositInput.maturityRate(),
                    depositInput.baseRate(), depositInput.elapsedMonths(), depositInput.contractMonths());
        } else if (urgentAmount == depositInput.principal()) {
            cancelAmount = depositInput.principal();
            aTotalLoss = DepositCalculator.calculateWithdrawalLoss(cancelAmount, depositInput.maturityRate(),
                    depositInput.baseRate(), depositInput.elapsedMonths(), depositInput.contractMonths());
        } else {
            // 급전 > 예금원금: 예금원금 해지손실 + 부족분 대출비용.
            // 부족분도 월납입으로 갚아나간다 — 매달 이자를 내고 남는 적립금이 부족분 원금에 도달하는
            // 시점에 완납한다고 본다(신용대출 방식1과 같은 논리, 예금 만기와는 무관).
            cancelAmount = depositInput.principal();
            long withdrawalLoss = DepositCalculator.calculateWithdrawalLoss(cancelAmount, depositInput.maturityRate(),
                    depositInput.baseRate(), depositInput.elapsedMonths(), depositInput.contractMonths());
            long shortfall = urgentAmount - depositInput.principal();
            LoanCostBreakdown shortfallLoan = computeShortfallLoanCost(shortfall, finalRate, monthlyPayment);
            aTotalLoss = withdrawalLoss + shortfallLoan.cost();
        }

        long cancelInterest = DepositCalculator.calculateEarlyWithdrawalInterest(cancelAmount, cancelInterestRate, depositInput.elapsedMonths());

        long bTotalLoss = mainLoan.cost();

        Winner winner;
        if (aTotalLoss < bTotalLoss) {
            winner = Winner.WITHDRAWAL;
        } else if (aTotalLoss > bTotalLoss) {
            winner = Winner.LOAN;
        } else {
            winner = Winner.TIE;
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

    // STEP 6 경우4(급전 > 예금원금)의 부족분 대출. 예금 만기와 무관하게 월납입으로 갚아나가다
    // 적립금(월납입-월이자)이 부족분 원금에 도달하는 시점(ceil)에 완납한다고 본다 — 신용대출이
    // 이 경우 방식1(월납입만으로 완납까지 시뮬레이션)로 처리하는 것과 같은 논리다.
    // 월적립이 0 이하면 영원히 못 갚으므로 PaymentTooLowException.
    private static LoanCostBreakdown computeShortfallLoanCost(long shortfall, BigDecimal finalRate, long monthlyPayment) {
        long monthlyInterest = computeMonthlyInterest(shortfall, finalRate);
        long monthlyAccumulation = monthlyPayment - monthlyInterest;
        if (monthlyAccumulation <= 0) {
            throw new PaymentTooLowException("부족분 대출의 월 상환 가능 금액이 월이자보다 적어 상환이 불가능합니다.");
        }

        int actualMonths = (int) ceilDiv(shortfall, monthlyAccumulation);
        int commitmentMonths = Math.max(MIN_COMMITMENT_MONTHS, actualMonths);

        long interest = monthlyInterest * actualMonths;
        long fee = computeFee(shortfall, actualMonths, commitmentMonths);

        return new LoanCostBreakdown(monthlyInterest, interest, fee, interest + fee);
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

    // STEP 5-1·5-2: 이자 지불 가능 여부 → 만기 상환 재원 확인. 하나라도 실패하면 PaymentTooLowException.
    private static void validateFeasibility(long urgentAmount, long monthlyPayment, LoanCostBreakdown mainLoan,
                                             int actualMonths, long depositMaturityAmount) {
        if (monthlyPayment < mainLoan.monthlyInterest()) {
            throw new PaymentTooLowException("월 상환 가능 금액이 월이자보다 적어 대출 상환이 불가능합니다.");
        }

        long accumulated = (monthlyPayment - mainLoan.monthlyInterest()) * actualMonths;
        long repaymentSource = accumulated + depositMaturityAmount;
        long amountDue = urgentAmount + mainLoan.fee();
        if (repaymentSource < amountDue) {
            throw new PaymentTooLowException("만기 상환 재원이 부족해 대출 상환이 불가능합니다.");
        }
    }

    private static long roundToWon(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
