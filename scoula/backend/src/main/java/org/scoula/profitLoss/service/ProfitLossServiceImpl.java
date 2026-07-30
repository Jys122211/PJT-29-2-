package org.scoula.profitLoss.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.profitLoss.dto.UserDepositDTO;
import org.scoula.profitLoss.mapper.ProfitLossMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class ProfitLossServiceImpl implements ProfitLossService {
    private final ProfitLossMapper mapper;

    @Override
    public List<UserDepositDTO> getDeposits(Long userId) {
        log.info("getDeposits..........userId={}", userId);

        return mapper.getDepositsByUserId(userId).stream()
                .map(UserDepositDTO::of)
                .toList();
    }

    @Override
    public List<Long> getQualifiedLoanProductIds(List<Long> qualificationQuestionIds) {
        List<Long> sanitizedQuestionIds = qualificationQuestionIds == null
                ? List.of()
                : qualificationQuestionIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        log.info(
                "getQualifiedLoanProductIds..........qualificationQuestionIds={}",
                sanitizedQuestionIds
        );

        return mapper.selectQualifiedLoanProductIds(
                sanitizedQuestionIds,
                sanitizedQuestionIds.size()
        );
    }

    @Override
    public BigDecimal getFinalDiscountRate(
            Long loanProductId,
            List<Long> preferentialQuestionIds
    ) {
        if (loanProductId == null) {
            throw new IllegalArgumentException("loanProductId는 필수입니다.");
        }

        List<Long> sanitizedQuestionIds = preferentialQuestionIds == null
                ? List.of()
                : preferentialQuestionIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        log.info(
                "getFinalDiscountRate..........loanProductId={}, preferentialQuestionIds={}",
                loanProductId,
                sanitizedQuestionIds
        );

        return Optional.ofNullable(
                mapper.selectFinalDiscountRate(
                        loanProductId,
                        sanitizedQuestionIds
                )
        ).orElse(BigDecimal.ZERO);
    }
}
