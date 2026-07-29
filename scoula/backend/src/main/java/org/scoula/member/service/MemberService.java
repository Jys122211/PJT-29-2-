package org.scoula.member.service;

import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.member.dto.MemberDTO;
import org.scoula.member.dto.MemberJoinDTO;
import org.scoula.member.dto.MemberUpdateDTO;

public interface MemberService {
    boolean checkDuplicate(String email);

    MemberDTO get(String email);

    MemberDTO getFirst();

    MemberDTO join(MemberJoinDTO member);

    MemberDTO update(MemberUpdateDTO member);

    void changePassword(ChangePasswordDTO changePassword);

    MemberDTO updateCreditScore(String email, Integer creditScore);

    MemberDTO updateMaxMonthlyPayment(String email, Integer maxMonthlyPayment);

}
