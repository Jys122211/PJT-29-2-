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
}
