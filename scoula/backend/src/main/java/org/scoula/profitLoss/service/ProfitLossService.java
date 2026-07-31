package org.scoula.profitLoss.service;

import org.scoula.profitLoss.dto.UserDepositDTO;

import java.math.BigDecimal;
import java.util.List;

public interface ProfitLossService {
    List<UserDepositDTO> getDeposits(Long userId);

    List<Long> getQualifiedLoanProductIds(List<Long> qualificationQuestionIds);

    BigDecimal getFinalDiscountRate(
            Long loanProductId,
            List<Long> preferentialQuestionIds
    );
}
