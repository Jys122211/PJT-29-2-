package org.scoula.profitLoss.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

// 득실 계산 로직 명세서 STEP 4 — 상환 시뮬레이션. Spring 빈 아님, DB 접근 없음.
public final class LoanSimulator {

    // 월이율 = 대출금리(%) / 100 / 12 = 대출금리 / 1200
    private static final BigDecimal MONTHLY_RATE_DIVISOR = new BigDecimal("1200");
    private static final int CALC_SCALE = 20;

    private LoanSimulator() {
    }

    public record Result(long loanInterest, int repaymentMonths) {
    }

    // 방식 1 — 월납입만 (만기목돈상환 X)
    public static Result 방식1(long loanAmount, BigDecimal loanRate, long monthlyPayment) {
        long firstMonthInterest = calculateMonthlyInterest(loanAmount, loanRate);
        if (monthlyPayment <= firstMonthInterest) {
            throw new PaymentTooLowException("월 상환 가능 금액이 첫 달 이자보다 적어 대출 상환이 불가능합니다.");
        }

        long balance = loanAmount;
        long loanInterest = 0;
        int months = 0;

        while (balance > 0) {
            long monthlyInterest = calculateMonthlyInterest(balance, loanRate);
            long actualPayment = Math.min(monthlyPayment, balance + monthlyInterest);
            loanInterest += monthlyInterest;
            balance = balance + monthlyInterest - actualPayment;
            months++;
        }

        return new Result(loanInterest, months);
    }

    private static long calculateMonthlyInterest(long balance, BigDecimal loanRate) {
        BigDecimal interest = BigDecimal.valueOf(balance)
                .multiply(loanRate)
                .divide(MONTHLY_RATE_DIVISOR, CALC_SCALE, RoundingMode.HALF_UP);
        return interest.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
