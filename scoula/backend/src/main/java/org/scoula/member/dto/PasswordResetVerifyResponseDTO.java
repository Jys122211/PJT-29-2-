package org.scoula.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2단계 응답 - 3단계에서 사용할 1회용 토큰을 돌려준다.
 * 프론트는 이 값을 화면 상태로만 들고 있다가 마지막 요청에 함께 보낸다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetVerifyResponseDTO {
    private String resetToken;
}
