package org.scoula.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 2단계 - 메일로 받은 6자리 인증번호를 검증한다. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetVerifyRequestDTO {
    private String email;
    private String code;
}
