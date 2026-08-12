package org.scoula.deposit.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDepositHistoryVO {
    private Long historyId;
    private String depositGlobalId;
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
    private String isDeleted;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private String changeType;
    private LocalDateTime changedAt;
    private Long changedBy;
}