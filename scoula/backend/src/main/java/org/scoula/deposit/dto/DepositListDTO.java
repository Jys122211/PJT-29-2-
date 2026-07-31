package org.scoula.deposit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 목록 조회 응답 래퍼.
 *
 * <p>totalPrincipal은 화면 02-03의 "총 보유 자산" 카드에 사용합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositListDTO {

    private int count;
    private long totalPrincipal;
    private List<DepositDTO> deposits;
}
