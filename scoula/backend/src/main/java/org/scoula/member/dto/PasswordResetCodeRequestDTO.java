package org.scoula.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 1단계 - 비밀번호 찾기 화면에서 이메일을 받아 인증번호 발송을 요청한다. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetCodeRequestDTO {
    private String email;
}
