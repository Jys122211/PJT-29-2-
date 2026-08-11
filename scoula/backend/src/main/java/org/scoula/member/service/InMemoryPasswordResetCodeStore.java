package org.scoula.member.service;

import org.scoula.member.support.PasswordResetEntry;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인증번호를 서버 메모리에 보관하는 구현체.
 *
 * DB 스키마를 건드리지 않기 위한 1차 구현이라 다음 한계가 있다.
 *  - 서버를 재시작하면 진행 중이던 요청이 모두 사라진다
 *  - 서버를 여러 대로 늘리면 인스턴스끼리 공유되지 않는다
 * 실제 운영에서는 password_reset 테이블이나 Redis로 교체하는 것이 맞다.
 */
@Component
public class InMemoryPasswordResetCodeStore implements PasswordResetCodeStore {

    // key = 이메일(소문자), value = 진행 상태
    private final Map<String, PasswordResetEntry> store = new ConcurrentHashMap<>();

    @Override
    public void save(String email, String codeHash, LocalDateTime codeExpiresAt) {
        // 재발송을 누르면 이전 인증번호는 즉시 무효가 된다.
        store.put(email, new PasswordResetEntry(email, codeHash, codeExpiresAt));
        purgeExpired();
    }

    @Override
    public Optional<PasswordResetEntry> find(String email) {
        return Optional.ofNullable(store.get(email));
    }

    @Override
    public void increaseAttempts(String email) {
        PasswordResetEntry entry = store.get(email);
        if (entry != null) {
            entry.setAttempts(entry.getAttempts() + 1);
        }
    }

    @Override
    public void bindResetToken(String email, String resetToken, LocalDateTime tokenExpiresAt) {
        PasswordResetEntry entry = store.get(email);
        if (entry != null) {
            entry.setResetToken(resetToken);
            entry.setTokenExpiresAt(tokenExpiresAt);
        }
    }

    @Override
    public Optional<PasswordResetEntry> findByResetToken(String resetToken) {
        if (resetToken == null || resetToken.trim().isEmpty()) {
            return Optional.empty();
        }
        return store.values().stream()
                .filter(entry -> resetToken.equals(entry.getResetToken()))
                .findFirst();
    }

    @Override
    public void remove(String email) {
        store.remove(email);
    }

    // 만료된 요청이 메모리에 계속 쌓이지 않도록 발급 시점마다 함께 정리한다.
    private void purgeExpired() {
        LocalDateTime now = LocalDateTime.now();
        store.values().removeIf(entry ->
                entry.isCodeExpired()
                        && (entry.getTokenExpiresAt() == null || now.isAfter(entry.getTokenExpiresAt())));
    }
}
