package org.scoula.profitLoss.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDepositVO {
    private Long userDepositId;
    private Long userId;
    private String bankName;
    private String productName;
    private String accountNumber;
    private LocalDate joinDate;
    private LocalDate maturityDate;
    private BigDecimal appliedRate;
    private BigDecimal baseRate;
    private Long principalAmount;
}
