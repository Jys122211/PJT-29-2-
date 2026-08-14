package org.scoula.profitLoss.service.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.extern.log4j.Log4j2;

// 득실 계산 로직 명세서 STEP 4 — 상환 시뮬레이션. Spring 빈 아님, DB 접근 없음.
@Log4j2
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
        requirePaymentCoversFirstMonthInterest(balance, loanRate, payment);

        SimulationStep step = simulate(balance, loanRate, payment, -1);
        return new Result(roundToWon(step.totalInterest()), step.months());
    }

    // 방식 2 — 만기 목돈상환. 구간 A(유효성 체크 겸 시뮬레이션) → 구간 B(목돈 상환) → 구간 C(잔액 월납입, 방식1과 동일 루프)
    public static Method2Result 방식2(long loanAmount, BigDecimal loanRate, long monthlyPayment,
                                       int depositRemainingMonths, long depositMaturityAmount) {
        BigDecimal balance = BigDecimal.valueOf(loanAmount);
        BigDecimal payment = BigDecimal.valueOf(monthlyPayment);

        // 구간 A: "대출금 ≤ 월납입 × 예금잔여기간" 같은 원금 나눗셈 근사는 이자를 무시해 부정확하므로 쓰지 않는다. 반드시 루프로 확인한다.
        SimulationStep phaseA = simulate(balance, loanRate, payment, depositRemainingMonths);

        if (phaseA.endingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            // 예금 만기 전에 완납 → 목돈상환 무의미 → 방식1 결과로 반환
            return new Method2Result(roundToWon(phaseA.totalInterest()), phaseA.months(), 0L, true);
        }

        // 구간 B: 목돈 상환. 반올림 없는 잔액 그대로 다음 구간에 넘긴다 — 구간을 나눠도 반올림은 최종 결과에서 1회만 한다.
        BigDecimal lumpSum = BigDecimal.valueOf(depositMaturityAmount).min(phaseA.endingBalance());
        long lumpSumPrincipal = roundToWon(lumpSum);
        BigDecimal remainingBalance = phaseA.endingBalance().subtract(lumpSum);
        
        log.info(String.format("[LoanSimulator] 만기 목돈 상환 발생: 상환금액 %d원 (현재잔액 %s원과 예금만기수령액 %d원 중 작은 값)", 
                lumpSumPrincipal, phaseA.endingBalance(), depositMaturityAmount));
        log.info(String.format("[LoanSimulator] 목돈 상환 후 잔액: %s원", remainingBalance));

        if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return new Method2Result(roundToWon(phaseA.totalInterest()), phaseA.months(), lumpSumPrincipal, false);
        }

        // 구간 C: 남은 잔액을 월납입으로 계속 (방식1과 동일 루프). 구간A 이자와 합산 후 최종 결과에서 한 번만 반올림한다.
        requirePaymentCoversFirstMonthInterest(remainingBalance, loanRate, payment);
        SimulationStep phaseC = simulate(remainingBalance, loanRate, payment, -1);
        BigDecimal totalInterest = phaseA.totalInterest().add(phaseC.totalInterest());

        return new Method2Result(roundToWon(totalInterest), phaseA.months() + phaseC.months(), lumpSumPrincipal, false);
    }

    private record SimulationStep(BigDecimal totalInterest, BigDecimal endingBalance, int months) {
    }

    // maxMonths < 0 이면 잔액이 0이 될 때까지, 아니면 최대 maxMonths회만 반복한다(구간 A용). 반올림 없이 소수 그대로 누적한다.
    private static SimulationStep simulate(BigDecimal startingBalance, BigDecimal loanRate, BigDecimal payment, int maxMonths) {
        BigDecimal balance = startingBalance;
        BigDecimal totalInterest = BigDecimal.ZERO;
        int months = 0;

        log.info("========== [LoanSimulator] 대출 상환 시뮬레이션 시작 ==========");
        log.info(String.format("대출원금(시작잔액): %s원, 대출금리: %s%%, 월납입액: %s원", startingBalance, loanRate, payment));

        while (balance.compareTo(BigDecimal.ZERO) > 0 && (maxMonths < 0 || months < maxMonths)) {
            BigDecimal interest = calculateMonthlyInterest(balance, loanRate);
            BigDecimal actualPayment = payment.min(balance.add(interest));
            
            log.info(String.format("[LoanSimulator] %d회차 이자 계산: %s원 (계산식: 이전잔액 %s원 × %s / 1200)", 
                    months + 1, interest, balance, loanRate));
            log.info(String.format("[LoanSimulator] %d회차 실제 납입액: %s원", months + 1, actualPayment));
            
            totalInterest = totalInterest.add(interest);
            balance = balance.add(interest).subtract(actualPayment);
            
            log.info(String.format("[LoanSimulator] %d회차 상환 후 잔액: %s원", months + 1, balance));
            months++;
        }

        log.info("========== [LoanSimulator] 대출 상환 시뮬레이션 종료 ==========");

        return new SimulationStep(totalInterest, balance, months);
    }

    private static void requirePaymentCoversFirstMonthInterest(BigDecimal balance, BigDecimal loanRate, BigDecimal payment) {
        if (payment.compareTo(calculateMonthlyInterest(balance, loanRate)) <= 0) {
            throw new PaymentTooLowException("월 상환 가능 금액이 첫 달 이자보다 적어 대출 상환이 불가능합니다.");
        }
    }

    private static BigDecimal calculateMonthlyInterest(BigDecimal balance, BigDecimal loanRate) {
        return balance.multiply(loanRate).divide(MONTHLY_RATE_DIVISOR, CALC_SCALE, RoundingMode.HALF_UP);
    }

    private static long roundToWon(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
