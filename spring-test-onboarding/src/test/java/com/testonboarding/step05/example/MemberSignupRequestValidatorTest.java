package com.testonboarding.step05.example;

import com.testonboarding.member.dto.MemberSignupRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Step 5 — example A] DTO 검증 규칙의 "순수 단위 테스트"
 *
 * Bean Validation 어노테이션(@NotBlank, @Size, @Pattern)은 웹 없이도 테스트할 수 있다.
 * Validator를 직접 만들어 DTO를 넣으면 위반 목록(ConstraintViolation)이 나온다.
 *
 * 왜 이렇게도 테스트하나?
 * - MockMvc 테스트(example B)는 "웹 계층을 통과하며 400이 나가는가"를 검증하고
 * - 이 테스트는 "검증 규칙 자체가 의도대로 선언됐는가"를 빠르게(ms) 검증한다
 * - 규칙이 많아질수록(케이스 폭발) 가벼운 쪽에 케이스를 몰아주는 게 경제적이다
 */
@DisplayName("회원가입 DTO 검증 규칙 (순수 Validator)")
class MemberSignupRequestValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        // Spring 없이 Bean Validation 구현체(Hibernate Validator)를 직접 부트스트랩
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("모든 규칙을 만족하면 위반이 없다")
    void validate_정상요청_위반없음() {
        // given
        MemberSignupRequest request = new MemberSignupRequest("newbie", "spring123!", "새내기");

        // when
        Set<ConstraintViolation<MemberSignupRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("아이디에 대문자/특수문자가 있으면 패턴 위반 1건")
    void validate_아이디패턴위반_사유메시지확인() {
        // given : 길이(4~20)는 만족하지만 패턴(영문 소문자+숫자)을 위반
        MemberSignupRequest request = new MemberSignupRequest("NEWBIE!", "spring123!", "새내기");

        // when
        Set<ConstraintViolation<MemberSignupRequest>> violations = validator.validate(request);

        // then : 정확히 1건 — 어떤 필드가, 왜
        assertThat(violations).hasSize(1);
        ConstraintViolation<MemberSignupRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("username");
        assertThat(violation.getMessage()).isEqualTo("아이디는 영문 소문자와 숫자만 사용할 수 있습니다");
    }

    @Test
    @DisplayName("닉네임이 빈 값이면 필수+길이 위반 2건이 동시에 나온다")
    void validate_닉네임빈값_위반2건() {
        // given
        MemberSignupRequest request = new MemberSignupRequest("newbie", "spring123!", "");

        // when
        Set<ConstraintViolation<MemberSignupRequest>> violations = validator.validate(request);

        // then : @NotBlank와 @Size(min=2) 둘 다 걸린다 — 검증은 모든 규칙을 한 번에 평가한다
        assertThat(violations).hasSize(2);
        assertThat(violations).allSatisfy(v ->
                assertThat(v.getPropertyPath().toString()).isEqualTo("nickname"));
    }
}
