package org.scoula.profitLoss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.profitLoss.enums.LoanType;
import org.scoula.profitLoss.service.calculator.ComparisonCalculator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 인터페이스 계약서 3장 응답 스펙(안상우 → 결과 화면). 결과 화면이 쓰는 모든 값을 포함한다.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonResponse {
    private Long comparisonId;
    private ComparisonCalculator.Winner winner;
    private Long savingAmount;
    private Long urgentAmount;
    private Long monthlyPayment;
    private LocalDateTime createdAt;

    private Badges badges;
    private Warning warning;
    private LoanInfo loan;
    private DepositInfo deposit;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Badges {
        private String recommended;
        private Boolean isPartialAllowed;
        private Boolean isLumpSum;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Warning {
        private Boolean isBelowMinimumWage;
        private Long minimumWageDaily;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoanInfo {
        private String name;
        private LoanType type;
        private BigDecimal interestRate;
        private Integer ratePeriodMonths;
        private Long interest;
        private Long penalty;
        private Long cost;
        private Boolean isRateEstimated;
        private Long finalBalance;
        private Long netProfit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DepositInfo {
        private String name;
        private String accountNumber;
        private Long maintainInterest;
        private BigDecimal cancelInterestRate;
        private Long cancelInterest;
        private Long withdrawalProfit;
        private Long finalBalance;
    }
}
