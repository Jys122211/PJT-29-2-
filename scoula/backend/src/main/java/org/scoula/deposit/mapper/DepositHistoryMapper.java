package org.scoula.deposit.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.deposit.domain.UserDepositHistoryVO;

import java.util.List;

public interface DepositHistoryMapper {

    /** user_deposits 현재 상태를 그대로 복사해 이력 1행을 남긴다. */
    int insertSnapshot(@Param("userDepositId") Long userDepositId,
                       @Param("changeType") String changeType,
                       @Param("changedBy") Long changedBy);

    List<UserDepositHistoryVO> selectByDepositId(
            @Param("userDepositId") Long userDepositId,
            @Param("userId") Long userId);
}