package org.scoula.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.member.dto.MemberDTO;
import org.scoula.member.dto.MemberJoinDTO;
import org.scoula.member.dto.MemberUpdateDTO;
import org.scoula.member.exception.PasswordMissmatchException;
import org.scoula.member.mapper.MemberMapper;
import org.scoula.security.account.domain.AuthVO;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    final PasswordEncoder passwordEncoder;
    final MemberMapper mapper;

    @Override
    public boolean checkDuplicate(String email) {
        MemberVO member = mapper.findByEmail(email);
        return member != null;
    }

    @Override
    public MemberDTO get(Long userId) {
        MemberVO member = Optional.ofNullable(mapper.get(userId))
                .orElseThrow(NoSuchElementException::new);
        return MemberDTO.of(member);
    }


    private void saveAvatar(MultipartFile avatar, String email) {
        //아바타 업로드
        if (avatar != null && !avatar.isEmpty()) {
            File dest = new File("c:/upload/avatar", email + ".png");
            try {
                avatar.transferTo(dest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Transactional
    @Override
    public MemberDTO join(MemberJoinDTO dto) {
        MemberVO member = dto.toVO();

        member.setPassword(passwordEncoder.encode(member.getPassword())); // 비밀번호 암호화
        mapper.insert(member);

        // Auth 테이블이 제거되었으므로 권한 insert는 생략

        saveAvatar(dto.getAvatar(), member.getEmail());

        return MemberDTO.of(mapper.findByEmail(member.getEmail()));
    }

    @Override
    public MemberDTO update(MemberUpdateDTO member) {
        MemberVO vo = mapper.findByEmail(member.getEmail());
        // update 시나리오에선 비밀번호 확인 로직을 생략하거나 다른 방법 필요할 수 있음
        // (member.getPassword()가 MemberUpdateDTO에 없으므로)
        // 만약 필요하다면 MemberUpdateDTO에 password 필드를 추가해야함.
        // 현재 MemberUpdateDTO에는 password 필드가 지워졌으므로 이 부분을 주석처리 하거나 제거.
        /* 
        if (!passwordEncoder.matches(member.getPassword(), vo.getPassword())) {  // 비밀번호 일치 확인
            throw new PasswordMissmatchException();
        }
        */

        mapper.update(member.toVO());
        saveAvatar(member.getAvatar(), member.getEmail());
        return MemberDTO.of(mapper.findByEmail(member.getEmail()));

    }

    @Override
    public void changePassword(ChangePasswordDTO changePassword) {
        MemberVO member = mapper.findByEmail(changePassword.getEmail());

        if (!passwordEncoder.matches(changePassword.getOldPassword(), member.getPassword())) {
            throw new PasswordMissmatchException();
        }

        changePassword.setNewPassword(passwordEncoder.encode(changePassword.getNewPassword()));

        mapper.updatePassword(changePassword);
    }

    /**
     * 신용점수 수정
     * @param userId 유저 ID
     * @param creditScore 변경할 신용점수
     * @return 업데이트된 유저 정보
     */
    @Override
    public MemberDTO updateCreditScore(Long userId, Integer creditScore) {
        mapper.updateCreditScore(userId, creditScore);
        return get(userId);
    }

    /**
     * 월 상환 가능 금액 수정
     * @param userId 유저 ID
     * @param maxMonthlyPayment 변경할 금액
     * @return 업데이트된 유저 정보
     */
    @Override
    public MemberDTO updateMaxMonthlyPayment(Long userId, Integer maxMonthlyPayment) {
        mapper.updateMaxMonthlyPayment(userId, maxMonthlyPayment);
        return get(userId);
    }

}
