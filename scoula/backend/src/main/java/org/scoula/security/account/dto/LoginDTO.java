package org.scoula.security.account.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;

import javax.servlet.http.HttpServletRequest;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LoginDTO {
    // 로그인 화면에서 JSON으로 전달되는 이메일과 비밀번호이다.
    private String email;
    private String password;

    // Spring Security 필터가 HttpServletRequest의 JSON Body를 LoginDTO로 변환할 때 사용한다.
    public static LoginDTO of(HttpServletRequest request) {
        ObjectMapper om = new ObjectMapper();
        try {
            return om.readValue(request.getInputStream(), LoginDTO.class);
        } catch (Exception e) {
            e.printStackTrace();
            // JSON 형식이 잘못됐거나 필수값을 읽지 못하면 로그인 실패로 처리한다.
            throw new BadCredentialsException("email 또는 password가 없습니다.");
        }
    }
}
