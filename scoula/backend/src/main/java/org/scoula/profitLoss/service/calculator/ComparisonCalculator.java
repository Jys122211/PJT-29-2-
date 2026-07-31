package org.scoula.profitLoss.service.calculator;

import java.math.BigDecimal;
import java.util.Map;

// 득실 계산 로직 명세서 STEP 5, 6 — 경우별 A·B 총손실과 최종 판정.
// DepositCalculator/LoanSimulator/LoanRateResolver/PrepaymentFeeCalculator를 조합하는 최상위 순수 클래스. Spring 빈 아님, DB 접근 없음.
public final class ComparisonCalculator {

    public enum Winner {
        WITHDRAWAL, LOAN, TIE
    }

    public record DepositInput(long principal, BigDecimal maturityRate, BigDecimal baseRate,
                                int contractMonths, int elapsedMonths) {
    }

    public record LoanResult(BigDecimal interestRate, int ratePeriodMonths, long interest, long penalty, long cost) {
    }

    public record DepositResult(BigDecimal cancelInterestRate, long cancelInterest, long maintainInterest) {
    }

    public record Result(long aTotalLoss, long bTotalLoss, Winner winner, long savingAmount,
                          LoanResult loan, DepositResult deposit) {
    }

    private record LoanCostBreakdown(BigDecimal loanRate, int ratePeriodMonths, long interest, long penalty, long cost) {
    }

    private ComparisonCalculator() {
    }

    public static Result compare(DepositInput depositInput, long urgentAmount, long monthlyPayment,
                                  boolean isPartialAllowed, boolean isLumpSum,
                                  Map<Integer, BigDecimal> ratesByPeriod) {
        // STEP 1: 예금잔여기간 = 계약월수 - 경과월수
        int depositRemainingMonths = depositInput.contractMonths() - depositInput.elapsedMonths();
        // STEP 2-5: 예금만기수령액 = 예금원금 + 만기이자(예금원금)
        long depositMaturityAmount = depositInput.principal()
                + DepositCalculator.calculateMaturityInterest(depositInput.principal(), depositInput.maturityRate(), depositInput.contractMonths());

        BigDecimal cancelInterestRate = DepositCalculator.calculateEarlyWithdrawalRate(
                depositInput.baseRate(), depositInput.elapsedMonths(), depositInput.contractMonths());
        long maintainInterest = DepositCalculator.calculateMaturityInterest(
                depositInput.principal(), depositInput.maturityRate(), depositInput.contractMonths());

        long aTotalLoss;
        long cancelAmount;

        if (urgentAmount < depositInput.principal()) {
            // 경우 1~4: 부분해지 O면 급전만, X면 예금원금 전액이 해지손실 대상
            cancelAmount = isPartialAllowed ? urgentAmount : depositInput.principal();
            aTotalLoss = DepositCalculator.calculateWithdrawalLoss(cancelAmount, depositInput.maturityRate(),
                    depositInput.baseRate(), depositInput.elapsedMonths(), depositInput.contractMonths());
        } else if (urgentAmount == depositInput.principal()) {
            // 경우 5·6
            cancelAmount = depositInput.principal();
            aTotalLoss = DepositCalculator.calculateWithdrawalLoss(cancelAmount, depositInput.maturityRate(),
                    depositInput.baseRate(), depositInput.elapsedMonths(), depositInput.contractMonths());
        } else {
            // 경우 7·8: 예금원금 해지손실 + 부족분(급전-예금원금) 대출비용. ★ 부족분 대출은 항상 방식1(예금을 깼으니 만기 목돈이 없다)
            cancelAmount = depositInput.principal();
            long withdrawalLoss = DepositCalculator.calculateWithdrawalLoss(cancelAmount, depositInput.maturityRate(),
                    depositInput.baseRate(), depositInput.elapsedMonths(), depositInput.contractMonths());
            long shortfall = urgentAmount - depositInput.principal();
            LoanCostBreakdown shortfallLoan = computeLoanCost(shortfall, monthlyPayment, false, 0, 0, ratesByPeriod);
            aTotalLoss = withdrawalLoss + shortfallLoan.cost();
        }

        long cancelInterest = DepositCalculator.calculateEarlyWithdrawalInterest(cancelAmount, cancelInterestRate, depositInput.elapsedMonths());

        // B안(급전 전액 대출)만 만기목돈상환 O/X를 따른다
        LoanCostBreakdown mainLoan = computeLoanCost(urgentAmount, monthlyPayment, isLumpSum,
                depositRemainingMonths, depositMaturityAmount, ratesByPeriod);
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

        LoanResult loanResult = new LoanResult(mainLoan.loanRate(), mainLoan.ratePeriodMonths(), mainLoan.interest(), mainLoan.penalty(), mainLoan.cost());
        DepositResult depositResult = new DepositResult(cancelInterestRate, cancelInterest, maintainInterest);

        return new Result(aTotalLoss, bTotalLoss, winner, savingAmount, loanResult, depositResult);
    }

    private static LoanCostBreakdown computeLoanCost(long loanAmount, long monthlyPayment, boolean isLumpSum,
                                                       int depositRemainingMonths, long depositMaturityAmount,
                                                       Map<Integer, BigDecimal> ratesByPeriod) {
        LoanRateResolver.Resolution resolution = LoanRateResolver.resolve(loanAmount, monthlyPayment, isLumpSum,
                depositRemainingMonths, depositMaturityAmount, ratesByPeriod);
        PrepaymentFeeCalculator.Result fee = PrepaymentFeeCalculator.calculate(loanAmount, resolution.loanRate(), monthlyPayment,
                resolution.loanInterest(), resolution.repaymentMonths(), resolution.lumpSumPrincipal(), depositRemainingMonths);
        return new LoanCostBreakdown(resolution.loanRate(), resolution.ratePeriodMonths(), resolution.loanInterest(), fee.totalFee(), fee.loanCost());
    }
}
