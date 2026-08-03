package org.scoula.profitLoss.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// 전세대출 계산 로직 명세서 STEP 3 "금리옵션" 한 행 = 상품 하나의 COFIX 유형(rate_type) 6종 중 하나.
// jeonse_rate_option × jeonse_product 조인. 신용대출과 달리 rate_period 개념이 없어 LoanProductRateVO를 재사용하지 않는다.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JeonseLoanProductVO {
    private Long productId;
    private String productName;
    private String rateType;
    private BigDecimal baseRate;
    private BigDecimal spreadRate;
    private Long maxLoanLimit;
}
