package org.scoula.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.member.exception.MailSendFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/** Gmail SMTP로 인증번호를 발송한다. */
@Component
@RequiredArgsConstructor
@Log4j2
public class SmtpPasswordResetMailSender implements PasswordResetMailSender {

    private final JavaMailSender mailSender;

    // 발신 계정. application.properties의 mail.username과 같은 값이다.
    @Value("${mail.username:}")
    private String from;

    // 받는 사람 메일함에 표시될 발신자 이름
    @Value("${mail.from.name:득실}")
    private String fromName;

    @Override
    public void send(String toEmail, String code, int expireMinutes) {
        // SMTP 계정이 아직 설정되지 않았으면 서버를 죽이지 않고 로그로만 남긴다.
        // 팀원이 각자 앱 비밀번호를 넣기 전에도 화면 흐름을 테스트할 수 있게 하기 위한 장치다.
        if (from == null || from.trim().isEmpty()) {
            log.warn("[비밀번호 찾기] SMTP 계정 미설정. 메일 대신 로그로 출력합니다. to={}, 인증번호={}", toEmail, code);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(new InternetAddress(from, fromName, "UTF-8"));
            helper.setTo(toEmail);
            helper.setSubject("[득실] 비밀번호 찾기 인증번호");
            helper.setText(buildBody(code, expireMinutes), false);

            mailSender.send(message);
            log.info("[비밀번호 찾기] 인증번호 메일 발송 완료. to={}", toEmail);
        } catch (Exception e) {
            // 인증번호가 로그에 남지 않도록 원인 예외만 기록한다.
            log.error("[비밀번호 찾기] 인증번호 메일 발송 실패. to={}", toEmail, e);
            throw new MailSendFailedException(e);
        }
    }

    private String buildBody(String code, int expireMinutes) {
        return "득실 비밀번호 찾기 인증번호입니다.\n\n"
                + "인증번호 : " + code + "\n\n"
                + "유효 시간은 " + expireMinutes + "분입니다.\n"
                + "본인이 요청하지 않았다면 이 메일을 무시해 주세요.\n";
    }
}
