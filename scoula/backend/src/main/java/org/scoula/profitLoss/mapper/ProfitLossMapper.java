package org.scoula.profitLoss.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.profitLoss.domain.UserDepositVO;

import java.util.List;

public interface ProfitLossMapper {
    List<UserDepositVO> getDepositsByUserId(@Param("userId") Long userId);
}
