package org.scoula.profitLoss.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.scoula.config.RootConfig;
import org.scoula.config.ServletConfig;
import org.scoula.profitLoss.service.ProfitLossService;
import org.scoula.profitLoss.service.ProfitLossServiceImpl;
import org.scoula.security.account.domain.AuthVO;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.domain.MemberVO;
import org.scoula.security.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        RootConfig.class,
        ServletConfig.class,
        SecurityConfig.class
})
@Log4j2
class ProfitLossControllerTest {

    @Autowired
    private ProfitLossService service;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("사용자 ID가 1인 사용자의 보유예금 목록 HTTP 조회")
    void getDepositsOfUserIdOne() throws Exception {
        Long targetUserId = 1L;
        assertInstanceOf(ProfitLossServiceImpl.class, service);

        Authentication authentication = createAuthentication(targetUserId);

        String responseContent = mockMvc.perform(
                        MockMvcRequestBuilders.get("/deposits/list")
                                .principal(authentication)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseBody = new ObjectMapper()
                .readTree(responseContent);

        assertTrue(responseBody.isArray());
        assertFalse(responseBody.isEmpty());

        log.info("responseContent={}", responseContent);
    }

    private Authentication createAuthentication(Long userId) {
        AuthVO memberAuthority = new AuthVO();
        memberAuthority.setUsername("deposit-user");
        memberAuthority.setAuth("ROLE_MEMBER");

        MemberVO member = MemberVO.builder()
                .userId(userId)
                .username("deposit-user")
                .password("test-password")
                .authList(List.of(memberAuthority))
                .build();
        CustomUser principal = new CustomUser(member);

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }
}
