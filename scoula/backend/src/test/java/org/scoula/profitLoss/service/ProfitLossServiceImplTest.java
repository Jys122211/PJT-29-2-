package org.scoula.profitLoss.service;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.scoula.config.RootConfig;
import org.scoula.profitLoss.dto.UserDepositDTO;
import org.scoula.security.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RootConfig.class, SecurityConfig.class})
@Log4j2
public class ProfitLossServiceImplTest {

    @Autowired
    private ProfitLossService service;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("사용자 ID가 1인 사용자의 보유예금 목록 불러오기")
    public void getDepositsOfUserIdOne() {
        Long targetUserId = 1L;
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        List<Long> expectedDepositIds = jdbcTemplate.queryForList(
                "SELECT user_deposit_id FROM user_deposits " +
                        "WHERE user_id = ? " +
                        "ORDER BY maturity_date ASC, user_deposit_id DESC",
                Long.class,
                targetUserId
        );

        assertFalse(expectedDepositIds.isEmpty(), "사용자 ID 1의 보유예금 테스트 데이터가 필요합니다.");

        List<UserDepositDTO> deposits = service.getDeposits(targetUserId);
        assertNotNull(deposits);

        List<Long> actualDepositIds = deposits.stream()
                .map(UserDepositDTO::getId)
                .toList();

        assertEquals(expectedDepositIds, actualDepositIds);
        assertTrue(deposits.stream().allMatch(deposit -> deposit.getBalance() > 0));
        deposits.forEach(log::info);
    }
}
