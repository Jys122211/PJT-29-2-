package org.scoula.member.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.member.dto.*;
import org.scoula.member.service.MemberService;
import org.scoula.security.account.domain.CustomUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

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

    private Long getAuthenticatedUserId(Principal principal) {
        if (principal instanceof Authentication) {
            Object authenticatedPrincipal = ((Authentication) principal).getPrincipal();
            if (authenticatedPrincipal instanceof CustomUser) {
                return ((CustomUser) authenticatedPrincipal).getMember().getUserId();
            }
        }
        return null;
    }

    /**
     * 내 프로필 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<MemberDTO> getMyProfile(Principal principal) {
        return ResponseEntity.ok(service.get(getAuthenticatedUserId(principal)));
    }

    /**
     * 내 신용점수 수정 (PATCH)
     */
    @PatchMapping("/me/credit-score")
    public ResponseEntity<MemberDTO> updateCreditScore(@RequestBody UpdateCreditScoreDTO dto, Principal principal) {
        Long userId = getAuthenticatedUserId(principal);
        return ResponseEntity.ok(service.updateCreditScore(userId, dto.getCreditScore()));
    }

    /**
     * 내 월 상환 가능 금액 수정 (PATCH)
     */
    @PatchMapping("/me/max-monthly-payment")
    public ResponseEntity<MemberDTO> updateMaxMonthlyPayment(@RequestBody UpdateMaxPaymentDTO dto,
            Principal principal) {
        Long userId = getAuthenticatedUserId(principal);
        return ResponseEntity.ok(service.updateMaxMonthlyPayment(userId, dto.getMaxMonthlyPayment()));
    }

    /**
     * 이메일 중복 체크
     */
    @GetMapping("/checkemail/{email}")
    public ResponseEntity<Boolean> checkEmail(@PathVariable String email) {
        return ResponseEntity.ok().body(service.checkDuplicate(email));
    }

    /**
     * 프로필 전체 수정 (PUT)
     */
    @PutMapping("/me")
    public ResponseEntity<MemberDTO> changeProfile(MemberUpdateDTO member, Principal principal) {
        return ResponseEntity.ok(service.update(getAuthenticatedUserId(principal), member));
    }

}
