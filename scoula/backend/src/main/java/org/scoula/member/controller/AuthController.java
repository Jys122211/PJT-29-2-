package org.scoula.member.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.member.dto.PasswordResetCodeRequestDTO;
import org.scoula.member.dto.PasswordResetRequestDTO;
import org.scoula.member.dto.PasswordResetVerifyRequestDTO;
import org.scoula.member.dto.PasswordResetVerifyResponseDTO;
import org.scoula.member.dto.SignupRequestDTO;
import org.scoula.member.dto.SignupResponseDTO;
import org.scoula.member.service.PasswordResetService;
import org.scoula.member.service.SignupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    // 실제 회원가입 규칙과 DB 저장 처리는 Service 계층에 위임한다.
    private final SignupService signupService;

    // 비밀번호 찾기 3단계(인증번호 발송 → 검증 → 변경)를 담당한다.
    private final PasswordResetService passwordResetService;

    /**
     * POST /api/auth/signup
     * 프런트에서 전달한 이름·이메일·비밀번호를 받아 회원가입을 처리한다.
     * 정상 등록되면 생성된 userId를 포함한 사용자 정보와 HTTP 201을 반환한다.
     */
    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDTO> signup(@RequestBody SignupRequestDTO request) {
        SignupResponseDTO response = signupService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/password/code
     * 비밀번호 찾기 1단계. 가입된 이메일이면 6자리 인증번호를 메일로 보낸다.
     * 재발송(인증번호 다시 보내기)도 같은 API를 호출하며, 이전 인증번호는 무효가 된다.
     * 가입되지 않은 이메일이면 404를 반환한다.
     */
    @PostMapping("/password/code")
    public ResponseEntity<Void> sendPasswordResetCode(@RequestBody PasswordResetCodeRequestDTO request) {
        passwordResetService.sendCode(request);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/auth/password/verify
     * 비밀번호 찾기 2단계. 인증번호가 맞으면 3단계에서 쓸 1회용 토큰을 반환한다.
     */
    @PostMapping("/password/verify")
    public ResponseEntity<PasswordResetVerifyResponseDTO> verifyPasswordResetCode(
            @RequestBody PasswordResetVerifyRequestDTO request) {
        return ResponseEntity.ok(passwordResetService.verifyCode(request));
    }

    /**
     * POST /api/auth/password/reset
     * 비밀번호 찾기 3단계. 1회용 토큰을 확인하고 새 비밀번호로 교체한다.
     */
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody PasswordResetRequestDTO request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
