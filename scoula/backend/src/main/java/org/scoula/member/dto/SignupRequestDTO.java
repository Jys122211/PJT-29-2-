package org.scoula.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequestDTO {
    // 회원가입 화면에서 백엔드로 전달되는 값들이다.
    private String name;
    private String email;

    // 요청에서는 평문 비밀번호를 받지만 DB에는 암호화된 password_hash만 저장한다.
    private String password;
}
