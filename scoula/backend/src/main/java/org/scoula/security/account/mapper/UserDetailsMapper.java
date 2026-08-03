package org.scoula.security.account.mapper;

import org.scoula.security.account.domain.MemberVO;
import org.apache.ibatis.annotations.Param;

public interface UserDetailsMapper {
    // 이메일로 로그인 대상 사용자의 비밀번호 해시·userId·권한을 조회한다.
    MemberVO get(@Param("email") String email);
}
