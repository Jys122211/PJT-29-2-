package org.scoula.security.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.scoula.security.util.JwtProcessor;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

    private JwtProcessor jwtProcessor;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtProcessor = new JwtProcessor();

        UserDetailsService userDetailsService = username -> User
                .withUsername(username)
                .password("unused")
                .authorities("ROLE_MEMBER")
                .build();

        filter = new JwtAuthenticationFilter(jwtProcessor, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bearerTokenCreatesAuthentication() throws Exception {
        String token = jwtProcessor.generateToken("deposit-user");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/profit-loss/deposits");
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertEquals("deposit-user", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_MEMBER")));
    }

    @Test
    void requestWithoutBearerTokenDoesNotCreateAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/profit-loss/deposits");

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
