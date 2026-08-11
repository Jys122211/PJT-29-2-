package org.scoula.member.exception;

/**
 * SMTP 발송이 실패했을 때 발생한다. ApiExceptionAdvice가 500으로 변환한다.
 * 원인 예외는 로그로만 남기고 사용자에게는 일반적인 안내만 보여준다.
 */
public class MailSendFailedException extends RuntimeException {
    public MailSendFailedException(Throwable cause) {
        super("인증번호 메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.", cause);
    }
}
