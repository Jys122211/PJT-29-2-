package org.scoula.profitLoss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JeonsePreferentialItemDTO {
    private Long id;
    private String conditionName;
    private String conditionDetail;
    private BigDecimal preferentialRate;
}
