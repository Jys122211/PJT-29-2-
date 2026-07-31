package org.scoula.profitLoss.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.profitLoss.dto.DepositListDTO;
import org.scoula.profitLoss.dto.UserDepositDTO;
import org.scoula.profitLoss.mapper.ProfitLossMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class ProfitLossServiceImpl implements ProfitLossService {

    final private ProfitLossMapper mapper;

    @Override
    public DepositListDTO getDeposits(Long userId) {
        log.info("getDeposits......" + userId);

        List<UserDepositDTO> deposits = mapper.getDepositsByUser(userId).stream()
                .map(UserDepositDTO::of)
                .toList();

        return new DepositListDTO(deposits.size(), deposits);
    }
}