package org.scoula.security.account.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.scoula.config.RootConfig;
import org.scoula.security.account.domain.MemberVO;
import org.scoula.security.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class, SecurityConfig.class})
class UserDetailsMapperTest {

    @Autowired
    private UserDetailsMapper mapper;

    @Test
    @DisplayName("로그인 사용자 admin의 userId를 tbl_member에서 조회")
    void getAdminWithUserId() {
        MemberVO member = mapper.get("admin");

        assertNotNull(member);
        assertEquals(1L, member.getUserId());
    }
}
