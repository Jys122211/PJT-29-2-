package org.scoula.security.filter;

import lombok.extern.log4j.Log4j2;
import org.scoula.security.account.dto.LoginDTO;
import org.scoula.security.handler.LoginFailureHandler;
import org.scoula.security.handler.LoginSuccessHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Log4j2
@Component
public class JwtUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    /**
     * 이메일·비밀번호 로그인 요청만 담당하는 Spring Security 필터이다.
     * 인증 결과에 따라 성공 또는 실패 Handler로 처리를 넘긴다.
     */
    public JwtUsernamePasswordAuthenticationFilter(
            AuthenticationManager authenticationManager,
            LoginSuccessHandler loginSuccessHandler,
            LoginFailureHandler loginFailureHandler) {
        super(authenticationManager);

        // POST /api/auth/login 요청이 들어왔을 때만 이 필터가 로그인 인증을 시도한다.
        setFilterProcessesUrl("/api/auth/login");
        setAuthenticationSuccessHandler(loginSuccessHandler);
        setAuthenticationFailureHandler(loginFailureHandler);
    }

    // 요청 JSON의 이메일·비밀번호를 Spring Security 인증 절차로 전달한다.
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        // 1. 요청 Body의 JSON에서 email과 password를 읽는다.
        LoginDTO login = LoginDTO.of(request);

        // 2. 아직 인증되지 않은 이메일·비밀번호 인증 객체를 만든다.
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(login.getEmail(), login.getPassword());

        // 3. AuthenticationManager가 사용자 조회와 BCrypt 비밀번호 비교를 수행한다.
        return getAuthenticationManager().authenticate(authenticationToken);
    }

}
