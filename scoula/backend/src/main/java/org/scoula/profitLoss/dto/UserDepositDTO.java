package org.scoula.profitLoss.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.profitLoss.domain.UserDepositVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDepositDTO {
    private Long userDepositId;
    private String bankName;
    private String productName;
    private Long principalAmount;
    private BigDecimal baseRate;
    private BigDecimal appliedRate;
    private String maturityDate;
    @JsonProperty("dDay")
    private long dDay;

    public static UserDepositDTO of(UserDepositVO vo) {
        return UserDepositDTO.builder()
                .userDepositId(vo.getUserDepositId())
                .bankName(vo.getBankName())
                .productName(vo.getProductName())
                .principalAmount(vo.getPrincipalAmount())
                .baseRate(vo.getBaseRate())
                .appliedRate(vo.getAppliedRate())
                .maturityDate(vo.getMaturityDate().toString())  // "2026-10-16" — 포맷은 프론트에서
                .dDay(ChronoUnit.DAYS.between(LocalDate.now(), vo.getMaturityDate()))
                .build();
    }
}