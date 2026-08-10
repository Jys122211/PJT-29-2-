package org.scoula.member.service;

/**
 * 인증번호 발송 방법을 추상화한다.
 * 지금은 Gmail SMTP를 쓰지만, 나중에 다른 발송 수단으로 바꿔도 서비스 코드는 그대로 둔다.
 */
public interface PasswordResetMailSender {
    void send(String toEmail, String code, int expireMinutes);
}
