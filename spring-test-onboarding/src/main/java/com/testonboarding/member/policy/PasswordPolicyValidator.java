package com.testonboarding.member.policy;

/**
 * 비밀번호 정책 검증기 (순수 자바 — Spring 의존 없음)
 *
 * 정책:
 * 1. null / 공백만으로 구성 불가
 * 2. 공백 문자 포함 불가
 * 3. 8자 이상
 * 4. 숫자 1개 이상 포함
 * 5. 특수문자(!@#$%^&*) 1개 이상 포함
 *
 * Step 1에서 이 클래스를 테스트합니다.
 * Spring 없이 new로 바로 만들 수 있는 클래스가 "테스트하기 가장 쉬운 코드"임을 보여주는 예시입니다.
 */
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 8;
    private static final String SPECIAL_CHARS = "!@#$%^&*";

    /**
     * 정책 위반 여부만 boolean으로 알려준다 (이유는 알려주지 않음)
     */
    public boolean isValid(String rawPassword) {
        try {
            validate(rawPassword);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 정책 위반 시 "무엇이 잘못됐는지" 메시지를 담아 예외를 던진다
     *
     * @throws IllegalArgumentException 정책 위반 시
     */
    public void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다");
        }
        if (rawPassword.contains(" ")) {
            throw new IllegalArgumentException("비밀번호에 공백을 포함할 수 없습니다");
        }
        if (rawPassword.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("비밀번호는 " + MIN_LENGTH + "자 이상이어야 합니다");
        }
        if (!containsDigit(rawPassword)) {
            throw new IllegalArgumentException("비밀번호에 숫자를 1개 이상 포함해야 합니다");
        }
        if (!containsSpecialChar(rawPassword)) {
            throw new IllegalArgumentException("비밀번호에 특수문자(" + SPECIAL_CHARS + ")를 1개 이상 포함해야 합니다");
        }
    }

    private boolean containsDigit(String value) {
        for (char c : value.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsSpecialChar(String value) {
        for (char c : value.toCharArray()) {
            if (SPECIAL_CHARS.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }
}
