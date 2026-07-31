package org.scoula.deposit.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.deposit.domain.UserDepositVO;

import java.util.List;

/**
 * 보유 예금 매퍼 (CRUD).
 *
 * <p>조회/수정/삭제에 userId를 함께 넘겨 WHERE 조건에 포함시킵니다.
 * 다른 사용자의 예금을 조작하지 못하게 하기 위함입니다.
 *
 * <p>파라미터가 2개 이상인 메서드는 @Param 이 필수입니다.
 */
public interface DepositMapper {

    List<UserDepositVO> getList(Long userId);

    int getCount(Long userId);

    UserDepositVO get(@Param("userDepositId") Long userDepositId,
                      @Param("userId") Long userId);

    void create(UserDepositVO deposit);

    int update(UserDepositVO deposit);

    int softDelete(@Param("userDepositId") Long userDepositId,
                   @Param("userId") Long userId);
}
