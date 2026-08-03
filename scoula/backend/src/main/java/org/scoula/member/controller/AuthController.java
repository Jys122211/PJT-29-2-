package org.scoula.member.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.member.dto.SignupRequestDTO;
import org.scoula.member.dto.SignupResponseDTO;
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
}
