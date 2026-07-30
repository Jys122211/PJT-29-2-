package org.scoula.security.account.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberVO {
    private Long userId;
    private String username;
    private String password;
    private String email;
    private String password; // password_hash 와 매핑
    private String name;
    private Integer creditScore;
    private Integer maxMonthlyPayment;
    private Boolean isDeleted;
    private Date createdAt;
    private String createdBy;
    private Date updatedAt;
    private String updatedBy;

    // 권한 목록 (users 테이블 구조상 별도 권한 테이블이 없다면 사용하지 않을 수 있으나 구조 유지를 위해 남겨둠)
    private List<AuthVO> authList; 
}
