package org.scoula.profitLoss.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// 득실 계산 로직 명세서 STEP 3 "대출 데이터" 한 행 = 상품 하나의 특정 rate_period(3/6/12) 금리 정보.
// credit_loan_grade_rate가 (상품 × 금리주기 × 등급) 조합마다 baseRate/spreadRate를 직접 갖고 있어
// 등급배율 계산이 필요 없다 — 최종금리 = baseRate + spreadRate − totalDiscountRate.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanProductRateVO {
    private Long loanProductId;
    private String productName;
    private Integer ratePeriodMonths;
    private BigDecimal baseRate;
    private BigDecimal spreadRate;
    private Long loanLimit;
}
