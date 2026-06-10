package com.testonboarding.step01.example;

import com.testonboarding.member.policy.PasswordPolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [Step 1 — example] 순수 JUnit5 + AssertJ 첫 테스트
 *
 * 이 테스트가 보여주는 것:
 * 1. 테스트는 "given(준비) → when(실행) → then(검증)" 3단계로 작성한다
 * 2. 테스트 메서드명은 {대상}_{시나리오}_{예상결과} — 이름만 읽어도 명세가 된다
 * 3. 단언(assertion)은 AssertJ의 assertThat / assertThatThrownBy 를 사용한다
 * 4. 같은 로직의 여러 입력은 @ParameterizedTest 로 묶는다 (복붙 금지)
 * 5. @Nested 로 시나리오를 그룹화하면 테스트 리포트가 문서처럼 읽힌다
 *
 * 주목할 점: 이 테스트에는 Spring이 전혀 없다.
 * PasswordPolicyValidator는 의존성이 없는 순수 자바 클래스라서
 * new 한 줄로 준비가 끝난다 — 이것이 "테스트하기 쉬운 코드"의 출발점이다.
 */
@DisplayName("비밀번호 정책 검증기")
class PasswordPolicyValidatorTest {

    private PasswordPolicyValidator validator;

    /**
     * 각 테스트 실행 전마다 새로 호출된다.
     * 테스트끼리 객체를 공유하지 않으므로 실행 순서와 무관하게 항상 같은 결과가 나온다(테스트 격리).
     */
    @BeforeEach
    void setUp() {
        validator = new PasswordPolicyValidator();
    }

    @Nested
    @DisplayName("isValid: 정책 통과 여부")
    class IsValid {

        @Test
        @DisplayName("모든 정책을 만족하면 true")
        void isValid_모든정책만족_true반환() {
            // given : 8자 이상 + 숫자 + 특수문자 포함
            String rawPassword = "spring123!";

            // when
            boolean result = validator.isValid(rawPassword);

            // then
            assertThat(result).isTrue();
        }

        /**
         * 같은 검증을 입력만 바꿔 반복할 때는 @ParameterizedTest.
         * 테스트 리포트에 입력값별로 한 줄씩 따로 표시되어 어떤 입력이 깨졌는지 바로 보인다.
         */
        @ParameterizedTest(name = "[{index}] \"{0}\" 은 정책 위반")
        @ValueSource(strings = {
                "short1!",        // 7자 — 길이 미달
                "spring1234",     // 특수문자 없음
                "springboot!",    // 숫자 없음
                "spring 123!"     // 공백 포함
        })
        @DisplayName("정책 위반 비밀번호는 false")
        void isValid_정책위반_false반환(String rawPassword) {
            // when
            boolean result = validator.isValid(rawPassword);

            // then
            assertThat(result).isFalse();
        }

        /**
         * null과 빈 문자열은 @NullAndEmptySource 한 줄로 함께 검증할 수 있다.
         */
        @ParameterizedTest(name = "[{index}] null/빈값은 정책 위반")
        @NullAndEmptySource
        void isValid_null또는빈값_false반환(String rawPassword) {
            assertThat(validator.isValid(rawPassword)).isFalse();
        }
    }

    @Nested
    @DisplayName("validate: 위반 사유를 예외 메시지로 알려준다")
    class Validate {

        /**
         * 예외 검증은 assertThatThrownBy 사용.
         * - isInstanceOf : 예외 타입 검증
         * - hasMessageContaining : "왜" 실패했는지(메시지)까지 검증해야
         *   사용자에게 잘못된 안내가 나가는 버그를 잡을 수 있다
         */
        @Test
        @DisplayName("길이 미달이면 '8자 이상' 메시지와 함께 예외")
        void validate_길이미달_예외발생() {
            // given
            String shortPassword = "abc1!";

            // when & then
            assertThatThrownBy(() -> validator.validate(shortPassword))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("8자 이상");
        }

        /**
         * 입력과 기대 메시지 쌍은 @CsvSource로 표처럼 관리한다.
         */
        @ParameterizedTest(name = "[{index}] \"{0}\" → \"{1}\" 메시지")
        @CsvSource({
                "spring1234,  특수문자",
                "springboot!, 숫자",
                "spring 123!, 공백"
        })
        @DisplayName("위반 유형별로 정확한 사유 메시지를 던진다")
        void validate_위반유형별_사유메시지(String rawPassword, String expectedKeyword) {
            assertThatThrownBy(() -> validator.validate(rawPassword))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(expectedKeyword);
        }

        @Test
        @DisplayName("정책을 모두 만족하면 예외가 발생하지 않는다")
        void validate_정책만족_예외없음() {
            // given
            String rawPassword = "spring123!";

            // when & then : 예외가 안 터지는 것 자체가 검증
            validator.validate(rawPassword);
        }
    }
}
