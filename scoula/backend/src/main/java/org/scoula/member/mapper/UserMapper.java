package org.scoula.member.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.member.domain.UserVO;

public interface UserMapper {
    // 동일한 이메일이 users 테이블에 이미 존재하는지 확인한다.
    boolean existsByEmail(String email);

    // 새 사용자를 저장한다. 생성된 user_id는 UserVO.userId에 자동으로 들어간다.
    int insert(UserVO user);

    // 비밀번호 찾기에서 가입 여부 확인과 userId 조회에 사용한다. 없으면 null을 반환한다.
    UserVO findByEmail(String email);

    // 비밀번호 찾기 마지막 단계에서 비밀번호 해시만 교체한다.
    int updatePasswordHash(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);
}
