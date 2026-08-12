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
        // rate_period는 대출 기간이 아니라 금리변동주기 — 짧을수록 금리가 싸므로 항상 가장
        // 짧은 3개월부터 시작해 실제 상환개월을 담을 수 있는 최소 구간을 찾는다. 초기 구간을
        // depositRemainingMonths 등으로 추정하면(조기 완납 시) 과대추정된 구간에 갇힌다.
        int ratePeriod = 3;

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

    private static int nextRatePeriod(int ratePeriod) {
        return ratePeriod == 3 ? 6 : 12;
    }
}
