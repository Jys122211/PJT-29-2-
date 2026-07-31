package org.scoula.profitLoss.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.profitLoss.enums.LoanType;
import org.scoula.profitLoss.service.calculator.ComparisonCalculator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// comparisons 테이블 21컬럼과 1:1 매핑 (scoula/backend/database.sql 참고)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonVO {
    private Long comparisonId;
    private Long userId;
    private Long userDepositId;
    private Long urgentAmount;
    private Long monthlyPayment;
    private Boolean isPartialAllowed;
    private Boolean isLumpSum;
    private String loanName;
    private LoanType loanType;
    private BigDecimal loanInterestRate;
    private Integer ratePeriodMonths;
    private Long loanInterest;
    private Long loanPenalty;
    private String depositName;
    private Long depositMaintainInterest;
    private BigDecimal depositCancelInterestRate;
    private Long depositCancelInterest;
    private Long aFinalBalance;
    private Long bFinalBalance;
    private ComparisonCalculator.Winner winner;
    private LocalDateTime createdAt;
}
