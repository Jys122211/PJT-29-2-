package org.scoula.profitLoss.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.profitLoss.dto.UserDepositDTO;
import org.scoula.profitLoss.mapper.ProfitLossMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
}
