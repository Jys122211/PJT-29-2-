package org.scoula.member.service;

import lombok.RequiredArgsConstructor;
import org.scoula.member.domain.UserVO;
import org.scoula.member.dto.SignupRequestDTO;
import org.scoula.member.dto.SignupResponseDTO;
import org.scoula.member.exception.DuplicateEmailException;
import org.scoula.member.exception.InvalidSignupRequestException;
import org.scoula.member.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {
    // 이메일이 "아이디@도메인.확장자" 형태인지 확인할 때 사용하는 정규식이다.
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    // 이메일 중복 조회와 사용자 INSERT를 담당하는 MyBatis Mapper이다.
    private final UserMapper userMapper;

    // 비밀번호를 평문으로 저장하지 않고 BCrypt 해시값으로 바꾸는 객체이다.
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public SignupResponseDTO signup(SignupRequestDTO request) {
        // 1. DB 작업 전에 필수값과 이메일 형식을 검사한다.
        validate(request);

        // 2. 앞뒤 공백을 제거한 이메일로 기존 가입 여부를 확인한다.
        String email = request.getEmail().trim();
        if (userMapper.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        // 3. 저장할 사용자 객체를 만든다.
        // 신용점수와 월 상환 가능 금액은 가입 후 프로필에서 입력하므로 여기서는 넣지 않는다.
        UserVO user = UserVO.builder()
                .name(request.getName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isDeleted("N")
                .build();

        // 4. users 테이블에 저장한다. 생성된 user_id는 user.userId에 자동으로 담긴다.
        userMapper.insert(user);

        // 5. 비밀번호 해시는 제외하고 userId·이름·이메일만 응답한다.
        return SignupResponseDTO.of(user);
    }

    // 잘못된 요청을 DB까지 보내지 않도록 회원가입 입력값을 검증한다.
    private void validate(SignupRequestDTO request) {
        if (request == null) {
            throw new InvalidSignupRequestException("회원가입 정보를 입력해 주세요.");
        }
        if (isBlank(request.getName())) {
            throw new InvalidSignupRequestException("이름을 입력해 주세요.");
        }
        if (isBlank(request.getEmail())) {
            throw new InvalidSignupRequestException("이메일을 입력해 주세요.");
        }
        if (!EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new InvalidSignupRequestException("올바른 이메일 형식으로 입력해 주세요.");
        }
        if (isBlank(request.getPassword())) {
            throw new InvalidSignupRequestException("비밀번호를 입력해 주세요.");
        }
    }

    // null, 빈 문자열, 공백만 있는 문자열을 모두 빈 값으로 판단한다.
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
