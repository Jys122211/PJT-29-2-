package org.scoula.security.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.dto.AuthResultDTO;
import org.scoula.security.account.dto.UserInfoDTO;
import org.scoula.security.util.JsonResponse;
import org.scoula.security.util.JwtProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Log4j2
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtProcessor jwtProcessor;

    // 로그인한 사용자의 이메일과 userId로 JWT를 만들고 응답 DTO에 담는다.
    private AuthResultDTO makeAuthResult(CustomUser user) {
        String username = user.getUsername();
        Long userId = user.getMember().getUserId();

        // JWT의 subject에는 이메일, 별도 claim에는 DB의 user_id가 들어간다.
        String token = jwtProcessor.generateToken(username, userId);

        // 프런트가 로그인 상태로 저장할 JWT와 사용자 기본 정보를 하나로 묶는다.
        return new AuthResultDTO(token, UserInfoDTO.of(user.getMember()));
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // AuthenticationManager가 인증한 실제 사용자 객체를 꺼낸다.
        CustomUser user = (CustomUser) authentication.getPrincipal();

        // 로그인 성공 결과를 JSON으로 프런트에 응답한다.
        AuthResultDTO result = makeAuthResult(user);
        JsonResponse.send(response, result);
    }
}

