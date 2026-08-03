package org.scoula.deposit.util;

import org.scoula.security.account.domain.CustomUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 로그인 사용자 정보 조회.
 *
 * <p>인증이 users 테이블로 이관되어 MemberVO에 userId가 있습니다.
 * ProfitLossController가 쓰는 방식과 동일하지만, Controller마다 캐스팅 코드를
 * 반복하지 않도록 이 클래스로 모았습니다.
 */
@Component
public class LoginUser {

    /**
     * 로그인한 사용자의 user_id.
     *
     * @throws IllegalStateException 인증 정보가 없는 경우
     */
    public static Long getUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUser)) {
            throw new IllegalStateException("로그인 정보를 확인할 수 없습니다");
        }

        CustomUser user = (CustomUser) authentication.getPrincipal();
        return user.getMember().getUserId();
    }
}
