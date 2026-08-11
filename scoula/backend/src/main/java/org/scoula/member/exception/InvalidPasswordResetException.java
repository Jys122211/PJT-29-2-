package org.scoula.member.exception;

/**
 * 인증번호가 틀렸거나 만료된 경우, 새 비밀번호가 규칙에 맞지 않는 경우 발생한다.
 * ApiExceptionAdvice가 400으로 변환하며, 메시지가 그대로 화면에 표시된다.
 */
public class InvalidPasswordResetException extends RuntimeException {
    public InvalidPasswordResetException(String message) {
        super(message);
    }
}
