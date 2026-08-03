package org.scoula.member.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.scoula.config.RootConfig;
import org.scoula.member.dto.SignupRequestDTO;
import org.scoula.member.dto.SignupResponseDTO;
import org.scoula.member.exception.DuplicateEmailException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        RootConfig.class,
        SignupServiceIntegrationTest.PasswordConfig.class
})
@Transactional
class SignupServiceIntegrationTest {
    @Configuration
    static class PasswordConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Autowired
    private SignupService signupService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void signupEncryptsPasswordAndLeavesProfileValuesEmpty() {
        String email = "signup-" + System.nanoTime() + "@example.com";
        String rawPassword = "1234abcd";

        SignupResponseDTO response = signupService.signup(
                new SignupRequestDTO("홍길동", email, rawPassword)
        );

        assertNotNull(response.getUserId());
        assertEquals("홍길동", response.getName());
        assertEquals(email, response.getEmail());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Map<String, Object> savedUser = jdbcTemplate.queryForMap(
                "SELECT password_hash, credit_score, max_monthly_payment, is_deleted " +
                        "FROM users WHERE user_id = ?",
                response.getUserId()
        );

        assertTrue(passwordEncoder.matches(rawPassword, (String) savedUser.get("password_hash")));
        assertNull(savedUser.get("credit_score"));
        assertNull(savedUser.get("max_monthly_payment"));
        assertEquals("N", savedUser.get("is_deleted"));

        assertThrows(
                DuplicateEmailException.class,
                () -> signupService.signup(
                        new SignupRequestDTO("중복 사용자", email, "anotherPassword")
                )
        );
    }
}
