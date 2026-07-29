package org.scoula.member.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpdateDTO {
    MultipartFile avatar;
    private String email;
    private String name;
    private Integer creditScore;
    private Integer maxMonthlyPayment;

    public MemberVO toVO() {
        return MemberVO.builder()
                .email(email)
                .name(name)
                .creditScore(creditScore)
                .maxMonthlyPayment(maxMonthlyPayment)
                .build();
    }
}
