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

    /**
     * 허용 문자 - 공백을 제외한 아스키 출력 가능 문자.
     * 영문 대소문자, 숫자, 특수문자가 모두 여기 들어간다.
     * 한글·이모지·공백은 걸러진다. 프론트의 정규식과 같은 범위여야 한다.
     */
    private static final java.util.regex.Pattern ALLOWED_PATTERN =
            java.util.regex.Pattern.compile("^[\\x21-\\x7E]+$");

    private PasswordPolicy() {
    }

    // 허용되지 않은 문자가 섞여 있는지 확인한다.
    public static boolean hasInvalidCharacter(String rawPassword) {
        return rawPassword != null
                && !rawPassword.isEmpty()
                && !ALLOWED_PATTERN.matcher(rawPassword).matches();
    }

    public static String invalidCharacterMessage() {
        return "비밀번호는 영문, 숫자, 특수문자만 입력할 수 있습니다.";
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
