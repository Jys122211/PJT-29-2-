package org.scoula.deposit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.deposit.domain.UserDepositVO;
import org.scoula.deposit.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 예금 등록/수정 요청 DTO.
 *
 * <p>날짜는 yyyyMMdd 8자리 문자열로 받아 LocalDate로 변환합니다.
 * 검증 실패 시 ValidationException을 던져 errorCode/field가 담긴 400 응답이 나갑니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequestDTO {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private String bankName;
    private String productName;
    private String accountNumber;
    private String joinDate;
    private String maturityDate;
    private Long principalAmount;
    private BigDecimal baseRate;
    private BigDecimal appliedRate;

    /**
     * 화면 07-02(필수값 누락), 07-03(날짜 역전) 검증.
     *
     * @throws ValidationException 검증 실패 시
     */
    public void validate() {
        requireText(bankName, "bankName", "은행명을 입력해주세요");
        requireText(productName, "productName", "상품명을 입력해주세요");
        requireText(accountNumber, "accountNumber", "계좌번호를 입력해주세요");
        requireText(joinDate, "joinDate", "가입일을 입력해주세요");
        requireText(maturityDate, "maturityDate", "만기일을 입력해주세요");
        requireBankName(bankName);
        requireProductName(productName);

        if (!accountNumber.trim().matches("\\d{14}")) {
            throw new ValidationException("INVALID_ACCOUNT_NUMBER", "accountNumber",
                    "KB 계좌번호는 숫자 14자리로 입력해주세요");
        }

        if (principalAmount == null || principalAmount <= 0) {
            throw new ValidationException("REQUIRED_FIELD", "principalAmount",
                    "가입금액을 입력해주세요");
        }
        if (principalAmount < 10_000L) {
            throw new ValidationException("INVALID_AMOUNT", "principalAmount",
                    "가입금액은 1만원 이상 입력해주세요");
        }
        if (principalAmount > 10_000_000_000L) {
            throw new ValidationException("INVALID_AMOUNT", "principalAmount",
                    "가입금액은 100억원 이하로 입력해주세요");
        }

        if (baseRate == null) {
            throw new ValidationException("REQUIRED_FIELD", "baseRate",
                    "기본금리를 입력해주세요");
        }
        if (appliedRate == null) {
            throw new ValidationException("REQUIRED_FIELD", "appliedRate",
                    "적용금리를 입력해주세요");
        }

        BigDecimal MAX_RATE = new BigDecimal("20");
        if (baseRate.compareTo(BigDecimal.ZERO) < 0 || baseRate.compareTo(MAX_RATE) > 0) {
            throw new ValidationException("INVALID_RATE", "baseRate",
                    "기본금리는 0~20% 사이로 입력해주세요");
        }
        if (appliedRate.compareTo(BigDecimal.ZERO) < 0 || appliedRate.compareTo(MAX_RATE) > 0) {
            throw new ValidationException("INVALID_RATE", "appliedRate",
                    "적용금리는 0~20% 사이로 입력해주세요");
        }

        if (baseRate.scale() > 2) {
            throw new ValidationException("INVALID_RATE", "baseRate",
                    "기본금리는 소수점 둘째 자리까지 입력해주세요");
        }
        if (appliedRate.scale() > 2) {
            throw new ValidationException("INVALID_RATE", "appliedRate",
                    "적용금리는 소수점 둘째 자리까지 입력해주세요");
        }

        // BigDecimal 비교는 compareTo 사용
        if (appliedRate.compareTo(baseRate) < 0) {
            throw new ValidationException("INVALID_RATE", "appliedRate",
                    "적용금리는 기본금리보다 크거나 같아야 합니다");
        }

        LocalDate join = parseDate(joinDate, "joinDate");
        LocalDate maturity = parseDate(maturityDate, "maturityDate");

        if (!maturity.isAfter(join)) {
            throw new ValidationException("INVALID_DATE", "maturityDate",
                    "만기일이 가입일보다 빠릅니다");
        }


    }

    /** 은행명 : 한글·영문·공백만. 실제 은행명에 숫자가 쓰이는 경우는 없다. */
    private void requireBankName(String value) {
        String v = value.trim();

        if (v.length() > 30) {
            throw new ValidationException("INVALID_LENGTH", "bankName",
                    "은행명은 30자 이내로 입력해주세요");
        }
        if (!v.matches("[가-힣a-zA-Z ]+")) {
            throw new ValidationException("INVALID_CHARACTER", "bankName",
                    "은행명은 한글과 영문만 입력할 수 있습니다");
        }
    }

    /**
     * 상품명 : 숫자를 허용한다. '26주적금', '369정기예금' 같은 실제 상품이 있다.
     * 다만 숫자나 기호만으로 이루어진 값은 막는다.
     */
    private void requireProductName(String value) {
        String v = value.trim();

        if (v.length() > 50) {
            throw new ValidationException("INVALID_LENGTH", "productName",
                    "상품명은 50자 이내로 입력해주세요");
        }
        if (!v.matches("[가-힣a-zA-Z0-9 ()\\-.]+")) {
            throw new ValidationException("INVALID_CHARACTER", "productName",
                    "상품명에 사용할 수 없는 문자가 있습니다");
        }
        if (!v.matches(".*[가-힣a-zA-Z].*")) {
            throw new ValidationException("INVALID_CHARACTER", "productName",
                    "상품명에 한글 또는 영문이 포함되어야 합니다");
        }
    }

    public UserDepositVO toVO() {
        return UserDepositVO.builder()
                .bankName(bankName.trim())
                .productName(productName.trim())
                .accountNumber(accountNumber.replaceAll("\\D", ""))
                .joinDate(LocalDate.parse(joinDate, FMT))
                .maturityDate(LocalDate.parse(maturityDate, FMT))
                .principalAmount(principalAmount)
                .baseRate(baseRate)
                .appliedRate(appliedRate)
                .build();
    }

    private void requireText(String value, String field, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("REQUIRED_FIELD", field, message);
        }
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value, FMT);
        } catch (DateTimeParseException e) {
            throw new ValidationException("INVALID_DATE", field,
                    "날짜 형식이 올바르지 않습니다 (yyyyMMdd)");
        }
    }
}
