package org.scoula.security.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResultDTO {
    // 로그인 성공 시 프런트에 반환하는 JWT이다.
    String token;

    // userId·이메일·권한 등 로그인한 사용자의 기본 정보이다.
    UserInfoDTO user;
}
