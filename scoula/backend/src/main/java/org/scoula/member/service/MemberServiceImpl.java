package org.scoula.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.member.dto.MemberDTO;
import org.scoula.member.dto.MemberJoinDTO;
import org.scoula.member.dto.MemberUpdateDTO;
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

import org.scoula.member.exception.DuplicateEmailException;

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
    public MemberDTO update(Long userId, MemberUpdateDTO member) {
        MemberVO currentMember = Optional.ofNullable(mapper.get(userId))
                .orElseThrow(NoSuchElementException::new);

        MemberVO updatedMember = member.toVO();
        updatedMember.setUserId(userId);
        mapper.update(updatedMember);
        saveAvatar(member.getAvatar(), currentMember.getEmail());

        return get(userId);
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
    public MemberDTO updateMaxMonthlyPayment(Long userId, Long maxMonthlyPayment) {
        mapper.updateMaxMonthlyPayment(userId, maxMonthlyPayment);
        return get(userId);
    }
    /**
     * 이름 수정
     * @param userId 유저 ID
     * @param name 변경할 이름
     * @return 업데이트된 유저 정보
     */
    @Override
    public MemberDTO updateName(Long userId, String name) {
        mapper.updateName(userId, name);
        return get(userId);
    }

    /**
     * 이메일 수정
     * @param userId 유저 ID
     * @param email 변경할 이메일
     * @return 업데이트된 유저 정보
     */
    @Override
    public MemberDTO updateEmail(Long userId, String email) {
        if (checkDuplicate(email)) {
            throw new DuplicateEmailException();
        }
        mapper.updateEmail(userId, email);
        return get(userId);
    }

}
