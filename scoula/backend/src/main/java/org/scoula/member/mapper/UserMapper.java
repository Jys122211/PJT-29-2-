package org.scoula.member.mapper;

import org.scoula.member.domain.UserVO;

public interface UserMapper {
    // 동일한 이메일이 users 테이블에 이미 존재하는지 확인한다.
    boolean existsByEmail(String email);

    // 새 사용자를 저장한다. 생성된 user_id는 UserVO.userId에 자동으로 들어간다.
    int insert(UserVO user);
}
