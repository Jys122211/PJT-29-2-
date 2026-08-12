package org.scoula.profitLoss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.profitLoss.constant.DepositTimeConstants;
import org.scoula.profitLoss.domain.UserDepositVO;
import org.scoula.deposit.util.AccountCrypto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDepositDTO {
    private Long id;
    private String bankName;
    private String productName;
    private String accountNumber;
    private LocalDate joinDate;
    private LocalDate maturityDate;
    private BigDecimal interestRate;
    private BigDecimal baseRate;
    private Long balance;
    private String maturityText;
    // 만기 당일은 제외 대상(expired=true) — isAfter가 당일을 false로 판정한다.
    // 클라이언트 로컬 타임존으로 판단하면 자정 부근에서 어긋나므로 서버가 확정해 내려준다.
    private Boolean expired;

    public static UserDepositDTO of(UserDepositVO vo) {
        return UserDepositDTO.builder()
                .id(vo.getUserDepositId())
                .bankName(vo.getBankName())
                .productName(vo.getProductName())
                .accountNumber(AccountCrypto.decrypt(vo.getAccountNumber()))
                .joinDate(vo.getJoinDate())
                .maturityDate(vo.getMaturityDate())
                .interestRate(vo.getAppliedRate())
                .baseRate(vo.getBaseRate())
                .balance(vo.getPrincipalAmount())
                .maturityText(makeMaturityText(vo.getJoinDate()))
                .expired(!vo.getMaturityDate().isAfter(LocalDate.now(DepositTimeConstants.DEPOSIT_TIMEZONE)))
                .build();
    }

    private static String makeMaturityText(LocalDate joinDate) {
        if (joinDate == null) {
            return "";
        }

        Period elapsed = Period.between(joinDate, LocalDate.now());
        long elapsedMonths = Math.max(0, elapsed.toTotalMonths());
        return String.format("가입 %d개월 경과", elapsedMonths);
    }
}
