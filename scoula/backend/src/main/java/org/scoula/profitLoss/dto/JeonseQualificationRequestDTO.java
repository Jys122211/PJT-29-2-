package org.scoula.profitLoss.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JeonseQualificationRequestDTO {
    private List<Long> qualificationQuestionIds;
}
