package org.scoula.security.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.scoula.config.RootConfig;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.config.SecurityConfig;
import org.scoula.security.handler.LoginSuccessHandler;
import org.scoula.security.util.JwtProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class, SecurityConfig.class})
@Transactional
class EmailAuthenticationIntegrationTest {
    private static final String EMAIL = "login-test@naver.com";
    private static final String PASSWORD = "password1234";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProcessor jwtProcessor;

    @Autowired
    private LoginSuccessHandler loginSuccessHandler;

    private JdbcTemplate jdbcTemplate;
    private Long userId;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", EMAIL);
        jdbcTemplate.update(
                """
                INSERT INTO users (
                    email,
                    password_hash,
                    name,
                    credit_score,
                    max_monthly_payment,
                    is_deleted,
                    created_at,
                    created_by
                )
                VALUES (?, ?, ?, NULL, NULL, 'N', NOW(), NULL)
                """,
                EMAIL,
                passwordEncoder.encode(PASSWORD),
                "로그인테스트"
        );
        userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM users WHERE email = ?",
                Long.class,
                EMAIL
        );
    }

    @Test
    void authenticatesWithEmailAndPassword() {
        Authentication result = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(EMAIL, PASSWORD)
        );

        assertTrue(result.isAuthenticated());
        assertEquals(EMAIL, result.getName());
        assertEquals(userId, ((CustomUser) result.getPrincipal()).getMember().getUserId());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority())));
    }

    @Test
    void rejectsWrongPassword() {
        assertThrows(
                BadCredentialsException.class,
                () -> authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(EMAIL, "wrong-password")
                )
        );
    }

    @Test
    void createsJwtWithEmailSubjectAndUserIdClaim() {
        String token = jwtProcessor.generateToken(EMAIL, userId);

        assertTrue(jwtProcessor.validateToken(token));
        assertEquals(EMAIL, jwtProcessor.getUsername(token));
        assertEquals(userId, jwtProcessor.getUserId(token));
    }

    @Test
    void loginSuccessResponseContainsUserId() throws Exception {
        Authentication result = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(EMAIL, PASSWORD)
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        loginSuccessHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                result
        );

        JsonNode responseBody = new ObjectMapper().readTree(response.getContentAsString());
        String token = responseBody.get("token").asText();

        assertEquals(userId.longValue(), responseBody.path("user").path("userId").asLong());
        assertEquals(userId, jwtProcessor.getUserId(token));
    }
}
