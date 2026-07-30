package org.scoula.profitLoss.service;

import org.scoula.profitLoss.dto.UserDepositDTO;

import java.util.List;

public interface ProfitLossService {
    List<UserDepositDTO> getDeposits(Long userId);
}
