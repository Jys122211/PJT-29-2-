package org.scoula.profitLoss.service;

import org.scoula.profitLoss.dto.ComparisonRequest;
import org.scoula.profitLoss.dto.ComparisonResponse;
import org.scoula.profitLoss.dto.ComparisonSummaryDTO;
import org.scoula.profitLoss.dto.UserDepositDTO;

import java.math.BigDecimal;
import java.util.List;

public interface ProfitLossService {

    // ── 입력 화면 (자금 입력 → 자격 확인 → 우대금리 확인)
    List<UserDepositDTO> getDeposits(Long userId);

    List<Long> getQualifiedLoanProductIds(List<Long> qualificationQuestionIds);

    BigDecimal getFinalDiscountRate(
            Long loanProductId,
            List<Long> preferentialQuestionIds
    );

    // ── 득실 비교
    ComparisonResponse compare(Long userId, ComparisonRequest request);

    ComparisonResponse getComparison(Long userId, Long comparisonId);

    // 득실 비교 히스토리
    List<ComparisonSummaryDTO> getComparisons(Long userId);
}
