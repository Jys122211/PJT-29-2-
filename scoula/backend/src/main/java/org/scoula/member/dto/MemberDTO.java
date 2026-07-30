package org.scoula.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDTO {
    private Long userId;
    private String email;
    private String name;
    private Integer creditScore;
    private Integer maxMonthlyPayment;
    private Date createdAt;
    private Date updatedAt;

    @JsonIgnore
    private MultipartFile avatar;

    private List<String> authList;

    public static MemberDTO of(MemberVO m) {
        return MemberDTO.builder()
                .userId(m.getUserId())
                .email(m.getEmail())
                .name(m.getName())
                .creditScore(m.getCreditScore())
                .maxMonthlyPayment(m.getMaxMonthlyPayment())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .authList(m.getAuthList() != null ? m.getAuthList().stream().map(a -> a.getAuth()).toList() : null)
                .build();
    }

    public MemberVO toVO() {
        return MemberVO.builder()
                .userId(userId)
                .email(email)
                .name(name)
                .creditScore(creditScore)
                .maxMonthlyPayment(maxMonthlyPayment)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}