package com.testonboarding.member.policy;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 닉네임 정책 검증기 (순수 자바 — Spring 의존 없음)
 *
 * 정책:
 * 1. null / 공백만으로 구성 불가
 * 2. 2자 이상 10자 이하
 * 3. 한글 / 영문 / 숫자만 허용 (특수문자, 공백 불가)
 * 4. 금지어("admin", "관리자") 포함 불가 — 대소문자 구분 없음
 *
 * Step 1의 연습문제(exercise) 대상 클래스입니다.
 * 학습자는 이 클래스의 테스트를 직접 작성합니다.
 */
public class NicknamePolicyValidator {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 10;
    private static final Pattern ALLOWED_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]+$");
    private static final List<String> BANNED_WORDS = Arrays.asList("admin", "관리자");

    public boolean isValid(String nickname) {
        try {
            validate(nickname);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * @throws IllegalArgumentException 정책 위반 시
     */
    public void validate(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 필수입니다");
        }
        if (nickname.length() < MIN_LENGTH || nickname.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "닉네임은 " + MIN_LENGTH + "자 이상 " + MAX_LENGTH + "자 이하여야 합니다");
        }
        if (!ALLOWED_PATTERN.matcher(nickname).matches()) {
            throw new IllegalArgumentException("닉네임은 한글/영문/숫자만 사용할 수 있습니다");
        }
        String lowered = nickname.toLowerCase();
        for (String banned : BANNED_WORDS) {
            if (lowered.contains(banned)) {
                throw new IllegalArgumentException("닉네임에 금지어를 포함할 수 없습니다: " + banned);
            }
        }
    }
}
