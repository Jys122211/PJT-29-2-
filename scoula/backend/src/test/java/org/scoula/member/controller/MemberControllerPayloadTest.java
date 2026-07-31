package org.scoula.member.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.scoula.member.dto.MemberDTO;
import org.scoula.member.service.MemberService;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberControllerPayloadTest {

    @Mock
    private MemberService service;

    @Test
    void updateMaxMonthlyPayment_acceptsNumericJsonValue() throws Exception {
        Long userId = 1L;
        Long monthlyPayment = 1_500_000L;
        CustomUser customUser = new CustomUser(MemberVO.builder()
                .userId(userId)
                .username("admin")
                .password("encoded-password")
                .build());
        var authentication = new UsernamePasswordAuthenticationToken(
                customUser,
                null,
                customUser.getAuthorities()
        );

        when(service.updateMaxMonthlyPayment(userId, monthlyPayment))
                .thenReturn(MemberDTO.builder()
                        .userId(userId)
                        .maxMonthlyPayment(monthlyPayment)
                        .build());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new MemberController(service))
                .build();

        mockMvc.perform(patch("/api/users/me/max-monthly-payment")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxMonthlyPayment\":1500000}"))
                .andExpect(status().isOk());

        verify(service).updateMaxMonthlyPayment(userId, monthlyPayment);
    }
}
