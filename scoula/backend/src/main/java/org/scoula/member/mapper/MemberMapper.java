package org.scoula.member.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.security.account.domain.AuthVO;
import org.scoula.security.account.domain.MemberVO;

public interface MemberMapper {
    MemberVO get(String email);

    // [TODO: 로그인 구현 후 삭제] 테스트용 임시 메서드
    MemberVO getFirst();

    MemberVO findByEmail(String email);    // email 중복 체크시 사용

    int insert(MemberVO member);  // 회원 정보 추가

    int insertAuth(AuthVO auth);        // 회원 권한 정보 추가

    int update(MemberVO member);

    int updatePassword(ChangePasswordDTO changePasswordDTO);

    int updateCreditScore(@Param("email") String email, @Param("creditScore") Integer creditScore);

    int updateMaxMonthlyPayment(@Param("email") String email, @Param("maxMonthlyPayment") Integer maxMonthlyPayment);

}
