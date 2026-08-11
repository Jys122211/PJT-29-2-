package org.scoula.member.support;

/**
 * 비밀번호 길이 규칙을 모아 둔 클래스.
 *
 * 회원가입(SignupServiceImpl)은 담당자가 따로 있어 이 클래스를 쓰지 않는다.
 * 값이 어긋나지 않도록 SignupServiceImpl의 상수, 프론트 JoinPage.vue의 PASSWORD_MAX_LENGTH와
 * 항상 같은 값으로 맞춰야 한다.
 */
public final class PasswordPolicy {

    // 비밀번호 최소 자릿수
    public static final int MIN_LENGTH = 4;

    // 비밀번호 최대 자릿수
    public static final int MAX_LENGTH = 16;

    private PasswordPolicy() {
    }

    // null은 여기서 판단하지 않고 호출부의 빈 값 검사에 맡긴다.
    public static boolean isTooShort(String rawPassword) {
        return rawPassword != null && rawPassword.length() < MIN_LENGTH;
    }

    public static boolean isTooLong(String rawPassword) {
        return rawPassword != null && rawPassword.length() > MAX_LENGTH;
    }

    public static String tooShortMessage() {
        return "비밀번호는 최소 " + MIN_LENGTH + "자 이상 입력해야 합니다.";
    }

    public static String tooLongMessage() {
        return "비밀번호는 최대 " + MAX_LENGTH + "자까지 입력할 수 있습니다.";
    }
}
