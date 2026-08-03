package org.scoula.member.exception;

// 이미 가입된 이메일로 회원가입을 요청했을 때 발생시키는 예외이다.
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("이미 가입된 이메일입니다.");
    }
}
