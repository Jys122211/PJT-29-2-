package org.scoula.profitLoss.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.profitLoss.enums.LoanType;

import java.math.BigDecimal;

// 득실 계산 로직 명세서 STEP 3 "대출 데이터" 한 행 = 상품 하나의 특정 rate_period(3/6/12) 금리 정보.
// credit_loan_products / credit_loan_grade_rate 테이블은 다른 팀원 담당이라 아직 없을 수 있다
// (ProfitLossMapper.xml TODO 참고). 등급배율 = gradeAverageSpreadRate / baseGradeAverageSpreadRate.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanProductRateVO {
    private Long loanProductId;
    private String productName;
    private LoanType loanType;
    private Integer ratePeriodMonths;
    private BigDecimal baseRate;
    private BigDecimal spreadRate;
    private BigDecimal preferentialRate;
    private BigDecimal gradeAverageSpreadRate;
    private BigDecimal baseGradeAverageSpreadRate;
}
