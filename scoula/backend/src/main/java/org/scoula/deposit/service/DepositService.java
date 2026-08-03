package org.scoula.deposit.service;

import org.scoula.deposit.dto.DepositDTO;
import org.scoula.deposit.dto.DepositListDTO;
import org.scoula.deposit.dto.DepositRequestDTO;

public interface DepositService {

    /** 화면 02-03, 07-09 */
    DepositListDTO getList();

    /** 화면 07-01 우상단 뱃지 */
    int getCount();

    /** 화면 07-10 진입 */
    DepositDTO get(Long userDepositId);

    /** 화면 07-01, 07-04, 07-07 */
    Long create(DepositRequestDTO request);

    /** 화면 07-10 */
    void update(Long userDepositId, DepositRequestDTO request);

    /** 화면 07-11 */
    void delete(Long userDepositId);
}
