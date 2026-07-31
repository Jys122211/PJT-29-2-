package org.scoula.profitLoss.mapper;

import org.scoula.profitLoss.domain.UserDepositVO;

import java.util.List;

public interface ProfitLossMapper {
    List<UserDepositVO> getDepositsByUser(Long userId);
}
