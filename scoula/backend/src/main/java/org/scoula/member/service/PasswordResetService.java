package org.scoula.member.service;

import org.scoula.member.dto.PasswordResetCodeRequestDTO;
import org.scoula.member.dto.PasswordResetRequestDTO;
import org.scoula.member.dto.PasswordResetVerifyRequestDTO;
import org.scoula.member.dto.PasswordResetVerifyResponseDTO;

/** 비밀번호 찾기 3단계 흐름을 담당한다. */
public interface PasswordResetService {

    // 1단계 : 가입 여부를 확인하고 6자리 인증번호를 메일로 보낸다.
    void sendCode(PasswordResetCodeRequestDTO request);

    // 2단계 : 인증번호를 검증하고 3단계에서 쓸 1회용 토큰을 발급한다.
    PasswordResetVerifyResponseDTO verifyCode(PasswordResetVerifyRequestDTO request);

    // 3단계 : 토큰을 확인하고 새 비밀번호로 교체한다.
    void resetPassword(PasswordResetRequestDTO request);
}
