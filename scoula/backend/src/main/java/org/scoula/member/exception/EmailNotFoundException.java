package org.scoula.member.exception;

/**
 * 비밀번호 찾기에서 입력한 이메일로 가입된 계정이 없을 때 발생한다.
 * ApiExceptionAdvice가 404로 변환한다.
 */
public class EmailNotFoundException extends RuntimeException {
    public EmailNotFoundException() {
        super("입력한 이메일로 가입된 계정을 찾을 수 없습니다.");
    }
}
