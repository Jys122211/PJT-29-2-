package org.scoula.deposit.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * user_deposits 테이블과 1:1 대응. (자산 등록/수정/삭제 - 화면 07-01 ~ 07-11)
 *
 * <p>profitLoss.domain.UserDepositVO와 같은 테이블을 읽지만 역할이 다릅니다.
 * 저쪽은 득실 계산용 읽기 전용이고, 이쪽은 CRUD 전체를 담당하므로
 * is_deleted, created_by 등 감사 컬럼까지 모두 가집니다.
 *
 * <p>mybatis-config.xml의 mapUnderscoreToCamelCase=true 로 자동 매핑됩니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDepositVO {

    private Long userDepositId;
    private String globalId;
    private Long userId;
    private String bankName;
    private String productName;
    private String accountNumber;
    private LocalDate joinDate;
    private LocalDate maturityDate;

    /** 금리는 부동소수점 오차를 피하기 위해 BigDecimal 사용 (DECIMAL(4,2)) */
    private BigDecimal appliedRate;
    private BigDecimal baseRate;

    private Long principalAmount;

    /** char(1) 'Y'/'N' 컬럼 */
    private String isDeleted;

    private Date createdAt;
    private Long createdBy;
    private Date updatedAt;
    private Long updatedBy;

    /** SQL에서 DATEDIFF로 계산해 내려주는 값. 실제 컬럼 아님 */
    private Integer dDay;
}
