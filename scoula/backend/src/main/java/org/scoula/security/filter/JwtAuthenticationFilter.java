package org.scoula.security.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.security.util.JwtProcessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Log4j2
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer "; // Bearer 뒤 공백까지 포함한다.

    private final JwtProcessor jwtProcessor;
    private final UserDetailsService userDetailsService;

    // JWT의 이메일을 이용해 사용자 정보를 다시 조회하고 인증 완료 객체를 만든다.
    private Authentication getAuthentication(String token) {
        String username = jwtProcessor.getUsername(token);
        UserDetails princiapl = userDetailsService.loadUserByUsername(username);
        log.info(princiapl);
        return new UsernamePasswordAuthenticationToken(princiapl, null, princiapl.getAuthorities());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 프런트가 보낸 "Authorization: Bearer {JWT}" 헤더를 읽는다.
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            // "Bearer " 문자를 제외하고 실제 JWT 문자열만 추출한다.
            String token = bearerToken.substring(BEARER_PREFIX.length());

            // JWT가 유효하면 현재 요청의 로그인 사용자 정보를 SecurityContext에 저장한다.
            Authentication authentication = getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // JWT가 없더라도 다음 필터로 요청을 넘긴다. 접근 허용 여부는 SecurityConfig가 판단한다.
        super.doFilter(request, response, filterChain);
    }
}
