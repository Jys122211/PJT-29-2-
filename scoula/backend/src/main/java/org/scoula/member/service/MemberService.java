package org.scoula.member.service;

import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.member.dto.MemberDTO;
import org.scoula.member.dto.MemberJoinDTO;
import org.scoula.member.dto.MemberUpdateDTO;

public interface MemberService {
    boolean checkDuplicate(String email);

    MemberDTO get(Long userId);

    MemberDTO join(MemberJoinDTO member);

    MemberDTO update(Long userId, MemberUpdateDTO member);

    void changePassword(ChangePasswordDTO changePassword);

    MemberDTO updateCreditScore(Long userId, Integer creditScore);

    MemberDTO updateMaxMonthlyPayment(Long userId, Long maxMonthlyPayment);

}
