package org.scoula.profitLoss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.profitLoss.domain.UserDepositVO;

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

    public static UserDepositDTO of(UserDepositVO vo) {
        return UserDepositDTO.builder()
                .id(vo.getUserDepositId())
                .bankName(vo.getBankName())
                .productName(vo.getProductName())
                .accountNumber(vo.getAccountNumber())
                .joinDate(vo.getJoinDate())
                .maturityDate(vo.getMaturityDate())
                .interestRate(vo.getAppliedRate())
                .baseRate(vo.getBaseRate())
                .balance(vo.getPrincipalAmount())
                .maturityText(makeMaturityText(vo.getJoinDate()))
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
