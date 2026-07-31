package org.scoula.profitLoss.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDepositVO {
    private Long userDepositId;
    private Long userId;
    private String bankName;
    private String productName;
    private LocalDate joinDate;
    private LocalDate maturityDate;
    private BigDecimal baseRate;
    private BigDecimal appliedRate;
    private Long principalAmount;
}
