package org.scoula.member.service;

import org.scoula.member.dto.SignupRequestDTO;
import org.scoula.member.dto.SignupResponseDTO;

public interface SignupService {
    // 회원가입 요청을 검증하고 users 테이블에 저장한 결과를 반환한다.
    SignupResponseDTO signup(SignupRequestDTO request);
}
