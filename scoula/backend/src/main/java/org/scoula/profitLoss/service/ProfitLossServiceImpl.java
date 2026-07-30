package org.scoula.profitLoss.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.profitLoss.dto.UserDepositDTO;
import org.scoula.profitLoss.mapper.ProfitLossMapper;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
