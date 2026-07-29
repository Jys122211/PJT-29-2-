package org.scoula.profitLoss.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

// 득실 계산 로직 명세서 STEP 4 — 상환 시뮬레이션. Spring 빈 아님, DB 접근 없음.
public final class LoanSimulator {

    // 월이율 = 대출금리(%) / 100 / 12 = 대출금리 / 1200
    private static final BigDecimal MONTHLY_RATE_DIVISOR = new BigDecimal("1200");
    // 반올림은 최종 결과에서만 한다 — 매달 반올림하면 누적 오차로 명세서 채점값과 어긋난다.
    private static final int CALC_SCALE = 20;

    private LoanSimulator() {
    }

    public record Result(long loanInterest, int repaymentMonths) {
    }

    public record Method2Result(long loanInterest, int repaymentMonths, long lumpSumPrincipal, boolean convertedToMethod1) {
    }

    // 방식 1 — 월납입만 (만기목돈상환 X)
    public static Result 방식1(long loanAmount, BigDecimal loanRate, long monthlyPayment) {
        BigDecimal balance = BigDecimal.valueOf(loanAmount);
        BigDecimal payment = BigDecimal.valueOf(monthlyPayment);

        if (payment.compareTo(calculateMonthlyInterest(balance, loanRate)) <= 0) {
            throw new PaymentTooLowException("월 상환 가능 금액이 첫 달 이자보다 적어 대출 상환이 불가능합니다.");
        }

        BigDecimal loanInterest = BigDecimal.ZERO;
        int months = 0;

        while (balance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal interest = calculateMonthlyInterest(balance, loanRate);
            BigDecimal actualPayment = payment.min(balance.add(interest));
            loanInterest = loanInterest.add(interest);
            balance = balance.add(interest).subtract(actualPayment);
            months++;
        }

        return new Result(roundToWon(loanInterest), months);
    }

    // 방식 2 — 만기 목돈상환. 구간 A(유효성 체크 겸 시뮬레이션) → 구간 B(목돈 상환) → 구간 C(잔액 월납입, 방식1과 동일 루프)
    public static Method2Result 방식2(long loanAmount, BigDecimal loanRate, long monthlyPayment,
                                       int depositRemainingMonths, long depositMaturityAmount) {
        BigDecimal balance = BigDecimal.valueOf(loanAmount);
        BigDecimal payment = BigDecimal.valueOf(monthlyPayment);
        BigDecimal loanInterest = BigDecimal.ZERO;
        int months = 0;

        // 구간 A: "대출금 ≤ 월납입 × 예금잔여기간" 같은 원금 나눗셈 근사는 이자를 무시해 부정확하므로 쓰지 않는다. 반드시 루프로 확인한다.
        for (int i = 0; i < depositRemainingMonths; i++) {
            BigDecimal interest = calculateMonthlyInterest(balance, loanRate);
            BigDecimal actualPayment = payment.min(balance.add(interest));
            loanInterest = loanInterest.add(interest);
            balance = balance.add(interest).subtract(actualPayment);
            months++;

            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                // 예금 만기 전에 완납 → 목돈상환 무의미 → 방식1 결과로 반환
                return new Method2Result(roundToWon(loanInterest), months, 0L, true);
            }
        }

        // 구간 B: 목돈 상환
        BigDecimal lumpSum = BigDecimal.valueOf(depositMaturityAmount).min(balance);
        long lumpSumPrincipal = roundToWon(lumpSum);
        balance = balance.subtract(lumpSum);

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return new Method2Result(roundToWon(loanInterest), months, lumpSumPrincipal, false);
        }

        // 구간 C: 남은 잔액을 월납입으로 계속 (방식1과 동일 루프)
        Result phaseC = 방식1(roundToWon(balance), loanRate, monthlyPayment);

        return new Method2Result(roundToWon(loanInterest) + phaseC.loanInterest(), months + phaseC.repaymentMonths(),
                lumpSumPrincipal, false);
    }

    private static BigDecimal calculateMonthlyInterest(BigDecimal balance, BigDecimal loanRate) {
        return balance.multiply(loanRate).divide(MONTHLY_RATE_DIVISOR, CALC_SCALE, RoundingMode.HALF_UP);
    }

    private static long roundToWon(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
