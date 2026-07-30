package org.scoula.security.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.security.account.domain.AuthVO;
import org.scoula.security.account.domain.MemberVO;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {
    String email;
    String name;
    Integer creditScore;
    Integer maxMonthlyPayment;
    List<String> roles;

    public static UserInfoDTO of(MemberVO member) {
        return new UserInfoDTO(
                member.getEmail(),
                member.getName(),
                member.getCreditScore(),
                member.getMaxMonthlyPayment(),
                member.getAuthList() != null ? member.getAuthList().stream().map(AuthVO::getAuth).toList() : List.of("ROLE_USER")
        );
    }
}
