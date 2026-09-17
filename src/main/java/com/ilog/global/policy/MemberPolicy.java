package com.ilog.global.policy;

import java.util.regex.Pattern;

/**
 * 회원 입력값 규칙. 미결 사항이 확정되면 이 클래스만 수정한다.
 *
 * <p>TODO(D-16): 비밀번호 정규식·닉네임 길이 확정 시 조정. [잠정] 비밀번호 8~20자 영문+숫자+특수문자, 닉네임 2~20자
 */
public final class MemberPolicy {

    public static final int EMAIL_MAX = 255;
    public static final int NAME_MAX = 50;

    public static final int NICKNAME_MIN = 2;
    public static final int NICKNAME_MAX = 20;

    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 20;

    /** 영문, 숫자, 특수문자(공백 제외)를 각각 1자 이상 포함한 8~20자. */
    public static final String PASSWORD_REGEX =
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])\\S{"
                    + PASSWORD_MIN
                    + ","
                    + PASSWORD_MAX
                    + "}$";

    public static final String PASSWORD_MESSAGE = "비밀번호는 영문, 숫자, 특수문자를 포함한 8~20자여야 합니다.";

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    private MemberPolicy() {}

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }
}
