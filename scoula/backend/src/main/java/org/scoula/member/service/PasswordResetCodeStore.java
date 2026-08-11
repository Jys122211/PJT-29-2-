package org.scoula.member.service;

import org.scoula.member.support.PasswordResetEntry;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 비밀번호 찾기 진행 상태를 보관하는 저장소.
 *
 * 지금은 InMemoryPasswordResetCodeStore(서버 메모리)를 쓴다.
 * 나중에 password_reset 테이블을 만들면 이 인터페이스를 구현한 클래스만 새로 만들고
 * @Component를 옮기면 서비스 코드는 그대로 둘 수 있다.
 */
public interface PasswordResetCodeStore {

    // 인증번호를 새로 발급한다. 같은 이메일의 이전 요청은 덮어쓴다.
    void save(String email, String codeHash, LocalDateTime codeExpiresAt);

    Optional<PasswordResetEntry> find(String email);

    // 인증번호가 틀렸을 때 시도 횟수를 1 올린다.
    void increaseAttempts(String email);

    // 인증 성공 후 1회용 토큰을 연결한다.
    void bindResetToken(String email, String resetToken, LocalDateTime tokenExpiresAt);

    Optional<PasswordResetEntry> findByResetToken(String resetToken);

    // 비밀번호 변경 완료·만료·시도 초과 시 폐기한다.
    void remove(String email);
}
