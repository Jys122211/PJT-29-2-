package org.scoula.profitLoss.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// 득실 계산 로직 명세서 STEP 2 "예금 데이터" 입력값. user_deposit 테이블은 다른 팀원 담당이라
// 아직 없을 수 있다 — 필드는 명세서가 요구하는 값 기준으로 가정했다 (ProfitLossMapper.xml TODO 참고).
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDepositVO {
    private Long userDepositId;
    private Long userId;
    private String productName;
    private Long principal;
    private BigDecimal maturityRate;
    private BigDecimal baseRate;
    private Integer contractMonths;
    private LocalDate joinDate;
}
