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
    // comparisons 컬럼이 아니다 — selectComparisonById가 user_deposits와 LEFT JOIN해서 채우거나
    // (GET), 서비스가 조회해둔 UserDepositVO에서 그대로 옮겨 담는다(POST). insertComparison에는 쓰지 않는다.
    private String depositAccountNumber;
    // depositAccountNumber와 같은 패턴 — 적용금리 표시용, comparisons 컬럼 아님.
    private BigDecimal depositAppliedRate;
    private Long depositMaintainInterest;
    private BigDecimal depositCancelInterestRate;
    private Long depositCancelInterest;
    private Long aFinalBalance;
    private Long bFinalBalance;
    private ComparisonCalculator.Winner winner;
    private LocalDateTime createdAt;
}
