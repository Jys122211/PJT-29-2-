package org.scoula.profitLoss.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.extern.log4j.Log4j2;

// 득실 계산 로직 명세서 STEP 4-3 — 약정기간·PMT·중도상환수수료·대출비용. Spring 빈 아님, DB 접근 없음.
// 세 기간을 혼동하지 말 것: rate_period(3/6/12, 금리 선택용) / 약정기간(PMT·수수료 분모용) / 실제상환개월(완납 시점, 방식2면 목돈 포함).
@Log4j2
public final class PrepaymentFeeCalculator {

    private static final BigDecimal PREPAYMENT_FEE_RATE = new BigDecimal("0.0011"); // 중도상환수수료율 0.11%
    private static final int CALC_SCALE = 20;

    public record Result(int termMonths, long pmt, long monthlyFeeTotal, long lumpSumFee, long totalFee, long loanCost) {
    }

    private PrepaymentFeeCalculator() {
    }

    public static Result calculate(long principal, BigDecimal loanRate, long monthlyPayment, long loanInterest,
                                    int actualRepaymentMonths, long lumpSumPrincipal, int depositRemainingMonths) {
        // ★ 약정기간은 만기목돈상환 O 케이스여도 반드시 방식1(순수 월납입)의 상환개월 기준으로 잡는다.
        //   방식2 실제개월(목돈 포함)을 쓰면 PMT가 커져 추가상환분이 음수가 되고 수수료가 0으로 조용히 틀린다 — 에러 없이 숫자만 틀리는 함정.
        int termMonths = Math.max(12, LoanSimulator.방식1(principal, loanRate, monthlyPayment).repaymentMonths());

        BigDecimal monthlyRate = loanRate.divide(new BigDecimal("1200"), CALC_SCALE, RoundingMode.HALF_UP);
        BigDecimal pmt = calculatePmt(principal, monthlyRate, termMonths);

        log.info("========== [PrepaymentFeeCalculator] 중도상환수수료 계산 시작 ==========");
        log.info(String.format("[PrepaymentFeeCalculator] 약정기간: %d개월, 약정 PMT(원리금균등액): %d원", termMonths, roundToWon(pmt)));

        // 일반 달: 실제납입 = 월납입(고정). 마지막 달만 실제납입이 다를 수 있어, 이미 알려진 합계(대출이자·원금·목돈상환원금)에서 역산한다.
        // 실제납입 합계 = 이자 합계 + 원금상환 합계이고, N-1개월은 월납입으로 고정이므로 마지막 달만 분리해 풀 수 있다.
        BigDecimal regularExtraPayment = BigDecimal.valueOf(monthlyPayment).subtract(pmt).max(BigDecimal.ZERO);

        long lastMonthPayment = loanInterest + (principal - lumpSumPrincipal)
                - (long) (actualRepaymentMonths - 1) * monthlyPayment;
        BigDecimal lastExtraPayment = BigDecimal.valueOf(lastMonthPayment).subtract(pmt).max(BigDecimal.ZERO);

        BigDecimal monthlyFeeSum = BigDecimal.ZERO;
        for (int t = 1; t <= actualRepaymentMonths; t++) {
            BigDecimal extraPayment = (t == actualRepaymentMonths) ? lastExtraPayment : regularExtraPayment;
            BigDecimal ratio = remainingTermRatio(termMonths, t);
            BigDecimal fee = extraPayment.multiply(PREPAYMENT_FEE_RATE).multiply(ratio);
            
            log.info(String.format("[PrepaymentFeeCalculator] %d회차 수수료: %d원 (계산식: 약정초과납입금 %d원 × 수수료율 0.0011 × 남은비율 %d/%d)",
                    t, roundToWon(fee), roundToWon(extraPayment), termMonths - t, termMonths));
                    
            monthlyFeeSum = monthlyFeeSum.add(fee);
        }

        BigDecimal lumpSumFeeExact = BigDecimal.ZERO;
        if (lumpSumPrincipal > 0) {
            // 목돈은 정상 스케줄에 없던 상환이므로 전액이 수수료 대상이다 (초과분이 아니라 원금 전체).
            BigDecimal ratio = remainingTermRatio(termMonths, depositRemainingMonths);
            lumpSumFeeExact = BigDecimal.valueOf(lumpSumPrincipal)
                    .multiply(PREPAYMENT_FEE_RATE)
                    .multiply(ratio);
                    
            log.info(String.format("[PrepaymentFeeCalculator] 목돈 상환 수수료: %d원 (계산식: 목돈원금 %d원 × 수수료율 0.0011 × 남은비율 %d/%d)",
                    roundToWon(lumpSumFeeExact), lumpSumPrincipal, termMonths - depositRemainingMonths, termMonths));
        }

        // 매달 수수료와 목돈 수수료를 각각 반올림해서 더하지 않는다 — 소수 그대로 합산한 뒤 수수료합에서 한 번만 반올림한다.
        long monthlyFeeTotal = roundToWon(monthlyFeeSum);
        long lumpSumFee = roundToWon(lumpSumFeeExact);
        long totalFee = roundToWon(monthlyFeeSum.add(lumpSumFeeExact));
        long loanCost = loanInterest + totalFee;

        log.info(String.format("[PrepaymentFeeCalculator] 산출 내역 요약 -> 총 월별수수료: %d원, 목돈수수료: %d원 => 합산 총 중도상환수수료: %d원", 
                monthlyFeeTotal, lumpSumFee, totalFee));
        log.info("========== [PrepaymentFeeCalculator] 중도상환수수료 계산 종료 ==========");

        return new Result(termMonths, roundToWon(pmt), monthlyFeeTotal, lumpSumFee, totalFee, loanCost);
    }

    // PMT = 원금 × 월이율 × (1+월이율)^약정 ÷ ((1+월이율)^약정 − 1). 수수료 기준선에만 쓰고 시뮬레이션에는 넣지 않는다.
    private static BigDecimal calculatePmt(long principal, BigDecimal monthlyRate, int termMonths) {
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(termMonths);
        BigDecimal numerator = BigDecimal.valueOf(principal).multiply(monthlyRate).multiply(factor);
        BigDecimal denominator = factor.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, CALC_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal remainingTermRatio(int termMonths, int elapsedMonths) {
        return BigDecimal.valueOf(termMonths - elapsedMonths)
                .divide(BigDecimal.valueOf(termMonths), CALC_SCALE, RoundingMode.HALF_UP);
    }

    private static long roundToWon(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
