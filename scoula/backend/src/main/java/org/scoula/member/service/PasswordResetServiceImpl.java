package org.scoula.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.member.domain.UserVO;
import org.scoula.member.dto.PasswordResetCodeRequestDTO;
import org.scoula.member.dto.PasswordResetRequestDTO;
import org.scoula.member.dto.PasswordResetVerifyRequestDTO;
import org.scoula.member.dto.PasswordResetVerifyResponseDTO;
import org.scoula.member.exception.EmailNotFoundException;
import org.scoula.member.exception.InvalidPasswordResetException;
import org.scoula.member.mapper.UserMapper;
import org.scoula.member.support.PasswordPolicy;
import org.scoula.member.support.PasswordResetEntry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class PasswordResetServiceImpl implements PasswordResetService {

    // 인증번호 유효 시간. 프론트 화면의 남은 시간 타이머와 같은 값이어야 한다.
    private static final int CODE_EXPIRE_MINUTES = 3;

    // 인증 성공 후 새 비밀번호를 입력할 수 있는 시간
    private static final int TOKEN_EXPIRE_MINUTES = 10;

    // 6자리 숫자는 100만 분의 1이라 시도 횟수를 막지 않으면 무차별 대입에 뚫린다.
    private static final int MAX_ATTEMPTS = 5;

    // 재발송 최소 간격. 프론트 FindPasswordPage.vue의 RESEND_COOLDOWN_SECONDS와 같은 값이어야 한다.
    // Gmail은 계정 하나당 하루 500통 제한이 있어, 연타로 한도를 소진하면 서비스 전체가 막힌다.
    private static final int RESEND_COOLDOWN_SECONDS = 30;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetCodeStore codeStore;
    private final PasswordResetMailSender mailSender;

    // 예측 가능한 Random 대신 암호학적 난수를 쓴다.
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void sendCode(PasswordResetCodeRequestDTO request) {
        String email = normalizeEmail(request == null ? null : request.getEmail());
        if (email.isEmpty()) {
            throw new InvalidPasswordResetException("이메일을 입력해 주세요.");
        }

        // 피그마 05번 화면 요구사항에 맞춰, 가입되지 않은 이메일이면 404로 알려준다.
        // 상용 서비스라면 계정 존재 여부가 새지 않도록 항상 성공 응답을 주는 편이 안전하다.
        UserVO user = userMapper.findByEmail(email);
        if (user == null) {
            throw new EmailNotFoundException();
        }

        // 직전 발급 후 일정 시간이 지나야 다시 보낼 수 있다.
        // 화면에서 버튼을 잠그지만, API를 직접 호출하는 경우를 대비해 서버에서도 확인한다.
        codeStore.find(email).ifPresent(entry -> {
            LocalDateTime issuedAt = entry.getCodeExpiresAt().minusMinutes(CODE_EXPIRE_MINUTES);
            LocalDateTime canResendAt = issuedAt.plusSeconds(RESEND_COOLDOWN_SECONDS);
            if (LocalDateTime.now().isBefore(canResendAt)) {
                throw new InvalidPasswordResetException(
                        "인증번호는 " + RESEND_COOLDOWN_SECONDS + "초 후에 다시 요청할 수 있습니다.");
            }
        });

        String code = generateCode();

        // 평문 인증번호는 저장하지 않는다. DB나 메모리가 노출돼도 바로 쓰이지 못하게 한다.
        codeStore.save(email,
                passwordEncoder.encode(code),
                LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));

        mailSender.send(email, code, CODE_EXPIRE_MINUTES);
    }

    @Override
    public PasswordResetVerifyResponseDTO verifyCode(PasswordResetVerifyRequestDTO request) {
        String email = normalizeEmail(request == null ? null : request.getEmail());
        String code = request == null || request.getCode() == null ? "" : request.getCode().trim();

        if (email.isEmpty() || code.isEmpty()) {
            throw new InvalidPasswordResetException("인증번호를 입력해 주세요.");
        }

        PasswordResetEntry entry = codeStore.find(email)
                .orElseThrow(() -> new InvalidPasswordResetException("인증번호를 다시 요청해 주세요."));

        if (entry.isCodeExpired()) {
            codeStore.remove(email);
            throw new InvalidPasswordResetException("인증번호가 만료되었습니다. 다시 요청해 주세요.");
        }

        if (entry.getAttempts() >= MAX_ATTEMPTS) {
            codeStore.remove(email);
            throw new InvalidPasswordResetException("인증 시도 횟수를 초과했습니다. 처음부터 다시 진행해 주세요.");
        }

        if (!passwordEncoder.matches(code, entry.getCodeHash())) {
            codeStore.increaseAttempts(email);
            throw new InvalidPasswordResetException("인증번호가 올바르지 않습니다.");
        }

        // 검증에 성공했으므로 3단계에서만 쓸 수 있는 1회용 토큰을 발급한다.
        String resetToken = UUID.randomUUID().toString();
        codeStore.bindResetToken(email, resetToken, LocalDateTime.now().plusMinutes(TOKEN_EXPIRE_MINUTES));

        return new PasswordResetVerifyResponseDTO(resetToken);
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetRequestDTO request) {
        String resetToken = request == null || request.getResetToken() == null
                ? "" : request.getResetToken().trim();
        String newPassword = request == null ? null : request.getNewPassword();

        PasswordResetEntry entry = codeStore.findByResetToken(resetToken)
                .orElseThrow(() -> new InvalidPasswordResetException("인증 정보가 없습니다. 처음부터 다시 진행해 주세요."));

        if (entry.isTokenExpired()) {
            codeStore.remove(entry.getEmail());
            throw new InvalidPasswordResetException("인증 유효 시간이 지났습니다. 처음부터 다시 진행해 주세요.");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new InvalidPasswordResetException("새 비밀번호를 입력해 주세요.");
        }
        if (PasswordPolicy.hasInvalidCharacter(newPassword)) {
            throw new InvalidPasswordResetException(PasswordPolicy.invalidCharacterMessage());
        }
        if (PasswordPolicy.isTooShort(newPassword)) {
            throw new InvalidPasswordResetException(PasswordPolicy.tooShortMessage());
        }
        if (PasswordPolicy.isTooLong(newPassword)) {
            throw new InvalidPasswordResetException(PasswordPolicy.tooLongMessage());
        }

        UserVO user = userMapper.findByEmail(entry.getEmail());
        if (user == null) {
            throw new EmailNotFoundException();
        }

        // 저장된 값은 BCrypt 해시라 문자열 비교가 안 된다. matches로 원문과 대조한다.
        // 프론트는 기존 비밀번호를 알 수 없으므로 이 검사는 서버에서만 가능하다.
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new InvalidPasswordResetException(
                    "현재 사용 중인 비밀번호와 동일합니다. " +
                            "다른 비밀번호를 입력해 주세요.");
        }

        userMapper.updatePasswordHash(user.getUserId(), passwordEncoder.encode(newPassword));

        // 같은 토큰으로 두 번 바꾸지 못하도록 즉시 폐기한다.
        codeStore.remove(entry.getEmail());
        log.info("[비밀번호 찾기] 비밀번호 변경 완료. userId={}", user.getUserId());
    }

    // 대소문자·공백 차이로 다른 사람 취급되지 않도록 이메일을 정규화한다.
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    // 000000 ~ 999999 범위의 6자리 문자열
    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
