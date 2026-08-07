package org.scoula.member.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.security.account.domain.AuthVO;
import org.scoula.security.account.domain.MemberVO;

public interface MemberMapper {
    MemberVO get(Long userId);

    MemberVO findByEmail(String email);    // email 중복 체크시 사용

    int insert(MemberVO member);  // 회원 정보 추가

    int insertAuth(AuthVO auth);        // 회원 권한 정보 추가

    int update(MemberVO member);

    int updateCreditScore(@Param("userId") Long userId, @Param("creditScore") Integer creditScore);

    int updateMaxMonthlyPayment(@Param("userId") Long userId, @Param("maxMonthlyPayment") Long maxMonthlyPayment);

}
