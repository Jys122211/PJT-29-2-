package org.scoula.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberJoinDTO {
    private String email;
    private String password;
    private String name;
    private Integer creditScore;
    private Long maxMonthlyPayment;

    private MultipartFile avatar;

    public MemberVO toVO() {
        return MemberVO.builder()
                .email(email)
                .password(password)
                .name(name)
                .creditScore(creditScore)
                .maxMonthlyPayment(maxMonthlyPayment)
                .build();
    }

}
