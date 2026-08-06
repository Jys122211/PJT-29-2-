package org.scoula.deposit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 은행 예금 화면에서 추출한 OCR 결과.
 * Gemini 원본 응답 구조를 프론트에 노출하지 않고 화면에 필요한 값만 전달합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrDepositResponseDTO {
    private String bankName;
    private String productName;
    private String accountNumber;
    private Long principalAmount;
    private String joinDate;
    private String maturityDate;
    private BigDecimal baseRate;
    private BigDecimal appliedRate;
}
