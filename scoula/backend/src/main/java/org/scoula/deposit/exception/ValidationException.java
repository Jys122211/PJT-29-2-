package org.scoula.deposit.exception;

import lombok.Getter;

/**
 * 입력값 검증 실패. 화면 07-02(필수값 누락), 07-03(날짜 역전) 대응.
 *
 * <p>errorCode와 field를 함께 담아 프론트가 해당 INPUT 하단에
 * 에러 메시지를 표시할 수 있게 합니다.
 */
@Getter
public class ValidationException extends RuntimeException {

    private final String errorCode;
    private final String field;

    public ValidationException(String errorCode, String field, String message) {
        super(message);
        this.errorCode = errorCode;
        this.field = field;
    }
}
