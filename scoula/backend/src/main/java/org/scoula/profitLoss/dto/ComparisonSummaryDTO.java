package org.scoula.profitLoss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 비교 내역 목록 카드 하나에 필요한 값만 담은 요약 DTO (상세 조회용 ComparisonResponse보다 가벼움)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonSummaryDTO {
    private Long comparisonId;
    private LocalDateTime createdAt;
    private Long urgentAmount;
    private String recommended;
    private Long savingAmount;
    private String depositName; // 추가
    private String loanTypeLabel; // 추가
    private String accountNumber;
}