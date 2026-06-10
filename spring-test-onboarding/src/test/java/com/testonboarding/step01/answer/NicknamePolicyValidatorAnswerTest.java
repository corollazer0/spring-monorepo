package com.testonboarding.step01.answer;

import com.testonboarding.member.policy.NicknamePolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [Step 1 — answer] NicknamePolicyValidatorExerciseTest 모범답안
 *
 * 채점 포인트:
 * - given/when/then 단계가 구분되어 있는가
 * - 예외 검증 시 타입뿐 아니라 메시지("왜")까지 확인했는가
 * - 반복 입력을 @ParameterizedTest로 처리했는가
 */
@DisplayName("닉네임 정책 검증기 (모범답안)")
class NicknamePolicyValidatorAnswerTest {

    private NicknamePolicyValidator validator;

    @BeforeEach
    void setUp() {
        // TODO 1 답: 순수 자바 클래스는 new가 전부 — 이래서 단위 테스트가 가장 빠르고 쉽다
        validator = new NicknamePolicyValidator();
    }

    @Test
    @DisplayName("정상 닉네임은 통과한다")
    void isValid_정상닉네임_true반환() {
        // given (TODO 2 답)
        String nickname = "테스터123";

        // when (TODO 3 답)
        boolean result = validator.isValid(nickname);

        // then (TODO 4 답)
        assertThat(result).isTrue();
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
        // when & then (TODO 5 답)
        assertThat(validator.isValid(nickname)).isFalse();
    }

    @Test
    @DisplayName("금지어가 들어가면 사유 메시지와 함께 예외가 발생한다")
    void validate_금지어포함_예외발생() {
        // given (TODO 6 답) : 대문자 Admin — 검증기는 소문자로 바꿔 비교하므로 이것도 막혀야 한다
        String nickname = "Admin짱";

        // when & then (TODO 7 답)
        assertThatThrownBy(() -> validator.validate(nickname))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("금지어");
    }
}
