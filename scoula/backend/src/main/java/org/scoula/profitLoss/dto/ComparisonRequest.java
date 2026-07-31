package org.scoula.profitLoss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.profitLoss.enums.LoanType;

import java.math.BigDecimal;
import java.util.List;

// 인터페이스 계약서 2장 요청 스펙(조윤상 → 안상우). 도메인 4그룹 중첩 구조.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonRequest {
    private UserFinancialInfo userFinancialInfo;
    private DepositCondition deposit;
    private LoanCondition loan;
    private ComparisonCondition comparisonCondition;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserFinancialInfo {
        private Long monthlyPayment;
        private Integer creditGrade;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DepositCondition {
        private Long userDepositId;
        // boolean이면 Jackson이 is 접두어를 떼어 매핑이 조용히 깨진다 — 반드시 Boolean 래퍼.
        private Boolean isPartialAllowed;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoanCondition {
        private List<Long> loanProductId;
        private LoanType loanType;
        private BigDecimal totalDiscountRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComparisonCondition {
        private Long urgentAmount;
        private Boolean isLumpSum;
    }
}
