package com.testonboarding.step01.exercise;

import com.testonboarding.member.policy.NicknamePolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * [Step 1 — exercise] 닉네임 정책 검증기 테스트를 직접 작성해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. 각 메서드의 TODO를 채운다 (example의 PasswordPolicyValidatorTest를 참고)
 * 3. .\gradlew :spring-test-onboarding:test 로 통과를 확인한다
 * 4. answer 패키지의 NicknamePolicyValidatorAnswerTest 와 비교한다
 *
 * 검증 대상 정책 (NicknamePolicyValidator):
 * - 2~10자, 한글/영문/숫자만, 금지어("admin", "관리자") 포함 불가
 */
@Disabled("과제: docs/test/education/FOR-Test-Step01.md 참고 후 @Disabled를 제거하고 완성하세요")
@DisplayName("닉네임 정책 검증기 (연습문제)")
class NicknamePolicyValidatorExerciseTest {

    private NicknamePolicyValidator validator;

    @BeforeEach
    void setUp() {
        // TODO 1: 검증기를 생성하세요 (Spring 없이 new로 충분합니다)
        validator = null;
    }

    @Test
    @DisplayName("정상 닉네임은 통과한다")
    void isValid_정상닉네임_true반환() {
        // given : 정책을 모두 만족하는 닉네임을 준비하세요
        // TODO 2

        // when : isValid를 호출하세요
        // TODO 3

        // then : assertThat(...).isTrue() 로 검증하세요
        // TODO 4
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" 은 정책 위반")
    @ValueSource(strings = {
            "a",            // 1자 — 길이 미달
            "열한글자가넘는닉네임이다",  // 10자 초과
            "nick name",    // 공백 포함
            "nick!"         // 특수문자 포함
    })
    @DisplayName("정책 위반 닉네임은 거부한다")
    void isValid_정책위반_false반환(String nickname) {
        // when & then : 한 줄로도 가능합니다
        // TODO 5
    }

    @Test
    @DisplayName("금지어가 들어가면 사유 메시지와 함께 예외가 발생한다")
    void validate_금지어포함_예외발생() {
        // given : "admin"이 들어간 닉네임 (대문자 Admin도 막혀야 합니다 — 직접 확인해보세요)
        // TODO 6

        // when & then : assertThatThrownBy로 예외 타입과 "금지어" 메시지를 검증하세요
        // TODO 7
    }
}
