package org.scoula.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 3단계 - 1회용 토큰과 새 비밀번호로 실제 변경을 요청한다. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestDTO {
    private String resetToken;
    private String newPassword;
}
