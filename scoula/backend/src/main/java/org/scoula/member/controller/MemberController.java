package org.scoula.member.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.common.util.UploadFiles;
import org.scoula.member.dto.ChangePasswordDTO;
import org.scoula.member.dto.MemberDTO;
import org.scoula.member.dto.MemberJoinDTO;
import org.scoula.member.dto.MemberUpdateDTO;
import org.scoula.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.security.Principal;

import org.scoula.member.dto.UpdateCreditScoreDTO;
import org.scoula.member.dto.UpdateMaxPaymentDTO;

/**
 * 회원 관리 API 컨트롤러
 * 프론트엔드 연동을 위해 /api/users 경로를 사용합니다.
 */
@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class MemberController {
    
    private final MemberService service;

    /**
     * 내 프로필 정보 조회
     * 토큰(Principal)이 없는 로컬 테스트 환경에서는 기본 1번 유저 정보를 반환합니다.
     */
    @GetMapping("/me")
    public ResponseEntity<MemberDTO> getMyProfile(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(service.getFirst());
        }
        return ResponseEntity.ok(service.get(principal.getName()));
    }

    /**
     * 내 신용점수 수정 (PATCH)
     */
    @PatchMapping("/me/credit-score")
    public ResponseEntity<MemberDTO> updateCreditScore(@RequestBody UpdateCreditScoreDTO dto, Principal principal) {
        String email = principal != null ? principal.getName() : service.getFirst().getEmail();
        return ResponseEntity.ok(service.updateCreditScore(email, dto.getCreditScore()));
    }

    /**
     * 내 월 상환 가능 금액 수정 (PATCH)
     */
    @PatchMapping("/me/max-monthly-payment")
    public ResponseEntity<MemberDTO> updateMaxMonthlyPayment(@RequestBody UpdateMaxPaymentDTO dto, Principal principal) {
        String email = principal != null ? principal.getName() : service.getFirst().getEmail();
        return ResponseEntity.ok(service.updateMaxMonthlyPayment(email, dto.getMaxMonthlyPayment()));
    }

    /**
     * 테스트용: 첫 번째 가입 유저 조회
     */
    @GetMapping("/first")
    public ResponseEntity<MemberDTO> getFirstProfile() {
        return ResponseEntity.ok(service.getFirst());
    }

    /**
     * 이메일 중복 체크
     */
    @GetMapping("/checkusername/{email}")
    public ResponseEntity<Boolean> checkUsername(@PathVariable String email) {
        return ResponseEntity.ok(service.checkDuplicate(email));
    }

    /**
     * 회원 가입
     */
    @PostMapping("")
    public ResponseEntity<MemberDTO> join(MemberJoinDTO member) {
        return ResponseEntity.ok(service.join(member));
    }

    /**
     * 유저 아바타 이미지 조회
     */
    @GetMapping("/{email}/avatar")
    public void getAvatar(@PathVariable String email, HttpServletResponse response) {
        String avatarPath = "c:/upload/avatar/" + email + ".png";
        File file = new File(avatarPath);
        
        // 아바타 이미지가 없을 경우 디폴트 이미지 제공
        if (!file.exists()) {
            file = new File("C:/upload/avatar/unknown.png");
        }
        UploadFiles.downloadImage(response, file);
    }

    /**
     * 프로필 전체 수정 (PUT)
     */
    @PutMapping("/{email}")
    public ResponseEntity<MemberDTO> changeProfile(MemberUpdateDTO member) {
        return ResponseEntity.ok(service.update(member));
    }

    /**
     * 비밀번호 변경
     */
    @PutMapping("/{email}/changepassword")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
        service.changePassword(changePasswordDTO);
        return ResponseEntity.ok().build();
    }
}
