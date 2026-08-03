package org.scoula.member.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    // users 테이블의 한 행을 자바 객체로 표현한 VO(Value Object)이다.
    private Long userId;
    private String email;
    private String passwordHash;
    private String name;

    // 가입 시에는 NULL이며 로그인 후 프로필 화면에서 사용자가 직접 입력한다.
    private Integer creditScore;
    private Long maxMonthlyPayment;

    // 사용자 상태와 등록·수정 정보를 기록하는 감사 컬럼이다.
    private String isDeleted;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
