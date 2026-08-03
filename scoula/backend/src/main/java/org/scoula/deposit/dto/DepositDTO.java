package org.scoula.deposit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.deposit.domain.UserDepositVO;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * 예금 응답 DTO.
 *
 * <p>VO를 그대로 응답하면 globalId, userId, isDeleted 등 내부 정보가 노출되므로
 * 명세서에 정의된 필드만 담아 내려보냅니다.
 *
 * <p>날짜는 yyyyMMdd 8자리 문자열로 변환합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositDTO {

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private Long userDepositId;
    private String bankName;
    private String productName;
    private String joinDate;
    private String maturityDate;
    private Long principalAmount;
    private BigDecimal baseRate;
    private BigDecimal appliedRate;

    /**
     * Lombok이 만드는 getter는 getDDay()이고, Jackson은 앞의 연속 대문자를
     * 소문자로 바꿔 "dday"로 직렬화합니다. 명세서의 dDay와 맞추기 위해 명시합니다.
     */
    @JsonProperty("dDay")
    private Integer dDay;

    public static DepositDTO of(UserDepositVO vo) {
        if (vo == null) {
            return null;
        }
        return DepositDTO.builder()
                .userDepositId(vo.getUserDepositId())
                .bankName(vo.getBankName())
                .productName(vo.getProductName())
                .joinDate(vo.getJoinDate() == null ? null : vo.getJoinDate().format(FMT))
                .maturityDate(vo.getMaturityDate() == null ? null : vo.getMaturityDate().format(FMT))
                .principalAmount(vo.getPrincipalAmount())
                .baseRate(vo.getBaseRate())
                .appliedRate(vo.getAppliedRate())
                .dDay(vo.getDDay())
                .build();
    }
}
