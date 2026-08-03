package org.scoula.security.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtProcessor {
    // 발급한 JWT는 1시간 동안 유효하다.
    static private final long TOKEN_VALID_MILISECOND = 1000L * 60 * 60; // 1시간

    // JWT 안에 DB 사용자 번호를 저장할 때 사용하는 claim 이름이다.
    static private final String USER_ID_CLAIM = "userId";

    // 서버가 JWT를 서명하고 검증할 때 사용하는 키이다.
    private final String secretKey = "충분히 긴 임의의(랜덤한) 비밀키 문자열 배정 ";
    private final Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

//    private Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);  -- 운영시 사용

    // 로그인 성공 시 이메일(subject)과 userId(claim)를 담은 JWT를 생성한다.
    public String generateToken(String subject, Long userId) {
        return Jwts.builder()
                .setSubject(subject)
                .claim(USER_ID_CLAIM, userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + TOKEN_VALID_MILISECOND))
                .signWith(key)
                .compact();
    }

    // JWT의 subject에 저장한 로그인 이메일을 꺼낸다.
    // 해석할 수 없는 토큰이면 예외가 발생하고 AuthenticationErrorFilter가 처리한다.
    // 예외 ExpiredJwtException, UnsupportedJwtException, MalformedJwtException, SignatureException,
    //      IllegalArgumentException
    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // JWT claim에 저장한 DB user_id를 Long 타입으로 꺼낸다.
    public Long getUserId(String token) {
        Object userId = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get(USER_ID_CLAIM);

        // JSON 숫자가 Integer 등 다른 Number 타입으로 읽혀도 Long으로 통일한다.
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }

        return Long.valueOf(userId.toString());
    }

    // JWT의 서명과 유효기간을 검증한다. 문제가 있으면 파싱 과정에서 예외가 발생한다.
    public boolean validateToken(String token) {
        Jws<Claims> claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
        return true;
    }

}
