package org.scoula.profitLoss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JeonseEligibilityQuestionDTO {
    private Long id;
    private String text;
}
