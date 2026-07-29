package org.scoula.profitLoss.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

// 득실 계산 로직 명세서 STEP 2 — 예금 관련 공식. Spring 빈 아님, DB 접근 없음.
public final class DepositCalculator {

    public static final BigDecimal INTEREST_INCOME_TAX_RATE = new BigDecimal("0.154");
    // 세후계수 = 1 - 이자소득세율. 예금 이자에만 적용한다 (대출이자·수수료엔 미적용).
    public static final BigDecimal AFTER_TAX_FACTOR = BigDecimal.ONE.subtract(INTEREST_INCOME_TAX_RATE);

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");
    // 나눗셈을 최종 1회로 미뤄 중간 반올림 오차를 없앤다. 반올림은 최종 결과에서만.
    private static final int CALC_SCALE = 20;

    private DepositCalculator() {
    }

    // STEP 2-1: 만기이자(세후) = 금액 × 만기이율 × (계약월수/12) × 0.846
    public static long calculateMaturityInterest(long amount, BigDecimal maturityRate, int contractMonths) {
        BigDecimal numerator = BigDecimal.valueOf(amount)
                .multiply(toDecimalRate(maturityRate))
                .multiply(BigDecimal.valueOf(contractMonths))
                .multiply(AFTER_TAX_FACTOR);
        return roundToWon(divideByTwelve(numerator));
    }

    // STEP 2-2: 중도해지이율(경과월수 구간표). 계산값 = 기본이율 × 구간율 × (경과월수/계약월수), 반환 = max(계산값, 최저금리)
    public static BigDecimal calculateEarlyWithdrawalRate(BigDecimal baseRate, int elapsedMonths, int contractMonths) {
        if (elapsedMonths < 1) {
            return new BigDecimal("0.100");
        }

        BigDecimal tierRate;
        BigDecimal minRate;
        if (elapsedMonths < 3) {
            tierRate = new BigDecimal("0.5");
            minRate = new BigDecimal("0.100");
        } else if (elapsedMonths < 6) {
            tierRate = new BigDecimal("0.5");
            minRate = new BigDecimal("0.100");
        } else if (elapsedMonths < 8) {
            tierRate = new BigDecimal("0.6");
            minRate = new BigDecimal("0.200");
        } else if (elapsedMonths < 10) {
            tierRate = new BigDecimal("0.7");
            minRate = new BigDecimal("0.200");
        } else if (elapsedMonths < 11) {
            tierRate = new BigDecimal("0.8");
            minRate = new BigDecimal("0.200");
        } else {
            tierRate = new BigDecimal("0.9");
            minRate = new BigDecimal("0.200");
        }

        BigDecimal calculated = baseRate
                .multiply(tierRate)
                .multiply(BigDecimal.valueOf(elapsedMonths))
                .divide(BigDecimal.valueOf(contractMonths), CALC_SCALE, RoundingMode.HALF_UP)
                .setScale(3, RoundingMode.HALF_UP);

        return calculated.max(minRate);
    }

    // STEP 2-3: 중도해지이자(세후) = 금액 × 중도해지이율 × (경과월수/12) × 0.846
    // ⚠ 분모는 "12" — calculateEarlyWithdrawalRate의 분모(계약월수)와 다르다. 계약이 12개월일 때만 우연히 같다.
    public static long calculateEarlyWithdrawalInterest(long amount, BigDecimal earlyWithdrawalRate, int elapsedMonths) {
        BigDecimal numerator = BigDecimal.valueOf(amount)
                .multiply(toDecimalRate(earlyWithdrawalRate))
                .multiply(BigDecimal.valueOf(elapsedMonths))
                .multiply(AFTER_TAX_FACTOR);
        return roundToWon(divideByTwelve(numerator));
    }

    // STEP 2-4: 해지손실(금액) = 만기이자(금액) − 중도해지이자(금액)
    public static long calculateWithdrawalLoss(long amount, BigDecimal maturityRate, BigDecimal baseRate,
                                                int elapsedMonths, int contractMonths) {
        long maturityInterest = calculateMaturityInterest(amount, maturityRate, contractMonths);
        BigDecimal earlyWithdrawalRate = calculateEarlyWithdrawalRate(baseRate, elapsedMonths, contractMonths);
        long earlyWithdrawalInterest = calculateEarlyWithdrawalInterest(amount, earlyWithdrawalRate, elapsedMonths);
        return maturityInterest - earlyWithdrawalInterest;
    }

    private static BigDecimal toDecimalRate(BigDecimal percentRate) {
        return percentRate.divide(ONE_HUNDRED, CALC_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal divideByTwelve(BigDecimal value) {
        return value.divide(MONTHS_PER_YEAR, CALC_SCALE, RoundingMode.HALF_UP);
    }

    private static long roundToWon(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
