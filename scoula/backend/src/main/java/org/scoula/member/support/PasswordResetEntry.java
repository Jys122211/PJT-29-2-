package org.scoula.member.support;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 비밀번호 찾기 1건의 진행 상태를 담는다.
 *
 * 지금은 서버 메모리에만 보관한다. 나중에 password_reset 테이블을 만들면
 * 이 클래스가 그대로 VO 역할을 하고 저장소 구현체만 교체하면 된다.
 */
@Getter
public class PasswordResetEntry {

    // 요청한 사용자의 이메일 (저장소의 key)
    private final String email;

    // 6자리 인증번호를 BCrypt로 해시한 값. 평문은 어디에도 남기지 않는다.
    private final String codeHash;

    // 인증번호 만료 시각 (발급 + 3분)
    private final LocalDateTime codeExpiresAt;

    // 인증번호 입력 시도 횟수. 무차별 대입을 막기 위해 제한한다.
    @Setter
    private int attempts;

    // 인증번호 검증에 성공한 뒤 발급되는 1회용 토큰
    @Setter
    private String resetToken;

    // 토큰 만료 시각 (발급 + 10분)
    @Setter
    private LocalDateTime tokenExpiresAt;

    public PasswordResetEntry(String email, String codeHash, LocalDateTime codeExpiresAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.codeExpiresAt = codeExpiresAt;
        this.attempts = 0;
    }

    public boolean isCodeExpired() {
        return LocalDateTime.now().isAfter(codeExpiresAt);
    }

    public boolean isTokenExpired() {
        return tokenExpiresAt == null || LocalDateTime.now().isAfter(tokenExpiresAt);
    }
}
