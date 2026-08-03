package org.scoula.deposit.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginUserTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserId_returnsAuthenticatedJwtUserId() {
        Long userId = 42L;
        CustomUser customUser = new CustomUser(MemberVO.builder()
                .userId(userId)
                .username("jwt-user@example.com")
                .password("encoded-password")
                .build());
        var authentication = new UsernamePasswordAuthenticationToken(
                customUser,
                null,
                customUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertEquals(userId, LoginUser.getUserId());
    }
}
