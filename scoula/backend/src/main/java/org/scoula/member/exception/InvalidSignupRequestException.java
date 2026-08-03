package org.scoula.member.exception;

// 이름·이메일·비밀번호가 비어 있거나 이메일 형식이 잘못됐을 때 발생시키는 예외이다.
public class InvalidSignupRequestException extends RuntimeException {
    public InvalidSignupRequestException(String message) {
        super(message);
    }
}
