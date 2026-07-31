package org.scoula.profitLoss.service.calculator;

import java.math.BigDecimal;
import java.util.Map;

// 득실 계산 로직 명세서 STEP 3 — 대출금리 & 실제 상환개월 확정(순환 해결). Spring 빈 아님, DB 접근 없음.
// base + spread×등급배율 − 우대 계산은 Service 책임이라, 여기서는 완성된 rate_period별 최종금리를 파라미터로 받는다.
public final class LoanRateResolver {

    public record Resolution(int ratePeriodMonths, BigDecimal loanRate, long loanInterest, int repaymentMonths,
                              long lumpSumPrincipal, boolean convertedToMethod1) {
    }

    private LoanRateResolver() {
    }

    public static Resolution resolve(long loanAmount, long monthlyPayment, boolean isLumpSum,
                                      int depositRemainingMonths, long depositMaturityAmount,
                                      Map<Integer, BigDecimal> ratesByPeriod) {
        int ratePeriod = selectInitialRatePeriod(loanAmount, monthlyPayment, isLumpSum,
                depositRemainingMonths, depositMaturityAmount);

        while (true) {
            BigDecimal rate = ratesByPeriod.get(ratePeriod);

            if (!isLumpSum) {
                LoanSimulator.Result result = LoanSimulator.방식1(loanAmount, rate, monthlyPayment);
                if (result.repaymentMonths() <= ratePeriod || ratePeriod == 12) {
                    return new Resolution(ratePeriod, rate, result.loanInterest(), result.repaymentMonths(), 0L, false);
                }
            } else {
                LoanSimulator.Method2Result result = LoanSimulator.방식2(loanAmount, rate, monthlyPayment,
                        depositRemainingMonths, depositMaturityAmount);
                if (result.repaymentMonths() <= ratePeriod || ratePeriod == 12) {
                    return new Resolution(ratePeriod, rate, result.loanInterest(), result.repaymentMonths(),
                            result.lumpSumPrincipal(), result.convertedToMethod1());
                }
            }

            // 실제상환개월 > rate_period → 다음 긴 구간으로 올려 재계산 (3 → 6 → 12)
            ratePeriod = nextRatePeriod(ratePeriod);
        }
    }

    // STEP 3-1: 임시 상환개월(이자 무시) → STEP 3-2: rate_period 임시 선택
    private static int selectInitialRatePeriod(long loanAmount, long monthlyPayment, boolean isLumpSum,
                                                int depositRemainingMonths, long depositMaturityAmount) {
        long tentativeMonths;
        if (!isLumpSum) {
            tentativeMonths = ceilDiv(loanAmount, monthlyPayment);
        } else {
            long remaining = loanAmount - depositMaturityAmount;
            tentativeMonths = remaining <= 0 ? 0 : ceilDiv(remaining, monthlyPayment);
            // 예금잔여기간보다 작으면(목돈만으로 만기 완납) → 예금잔여기간
            if (tentativeMonths < depositRemainingMonths) {
                tentativeMonths = depositRemainingMonths;
            }
        }
        return toRatePeriod(tentativeMonths);
    }

    private static int toRatePeriod(long tentativeMonths) {
        if (tentativeMonths <= 3) {
            return 3;
        }
        if (tentativeMonths <= 6) {
            return 6;
        }
        return 12;
    }

    private static int nextRatePeriod(int ratePeriod) {
        return ratePeriod == 3 ? 6 : 12;
    }

    private static long ceilDiv(long numerator, long denominator) {
        return (numerator + denominator - 1) / denominator;
    }
}
