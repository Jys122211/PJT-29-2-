package org.scoula.profitLoss.mapper;


import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.scoula.config.RootConfig;
import org.scoula.profitLoss.domain.UserDepositVO;
import org.scoula.security.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class, SecurityConfig.class})
@Log4j2
class ProfitLossMapperTest {
    @Autowired
    private ProfitLossMapper mapper;

    @Test
    @DisplayName("사용자 ID가 1인 사용자의 보유예금 목록 조회")
    public void getDepositsByUserId() {
        List<UserDepositVO> deposits = mapper.getDepositsByUserId(1L);
        assertFalse(deposits.isEmpty());
        deposits.forEach(log::info);
    }

    @Test
    @DisplayName("신청 자격 질문 ID 1, 2를 만족하는 신용대출 상품 ID 조회")
    public void selectQualifiedLoanProductIds() {
        List<Long> loanProductIds = mapper.selectQualifiedLoanProductIds(
                List.of(1L, 2L),
                2
        );

        assertNotNull(loanProductIds);
        loanProductIds.forEach(log::info);
    }

    @Test
    @DisplayName("신청 자격 질문 ID가 비어 있으면 빈 신용대출 상품 목록 반환")
    public void selectQualifiedLoanProductIdsWithEmptyQuestionIds() {
        List<Long> loanProductIds = mapper.selectQualifiedLoanProductIds(
                List.of(),
                0
        );

        assertNotNull(loanProductIds);
        assertTrue(loanProductIds.isEmpty());
    }
}
