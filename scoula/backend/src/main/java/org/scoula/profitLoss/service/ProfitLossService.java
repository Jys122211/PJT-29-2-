package org.scoula.profitLoss.service;

import org.scoula.profitLoss.dto.DepositListDTO;

public interface ProfitLossService {
    DepositListDTO getDeposits(Long userId);
}
