package org.scoula.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.member.domain.UserVO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponseDTO {
    // 회원가입 성공 후 프런트에 돌려주는 안전한 사용자 정보이다.
    private Long userId;
    private String name;
    private String email;

    // DB 저장에 사용한 UserVO를 응답 전용 DTO로 변환한다.
    // passwordHash 같은 민감정보는 응답에 포함하지 않는다.
    public static SignupResponseDTO of(UserVO user) {
        return SignupResponseDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
