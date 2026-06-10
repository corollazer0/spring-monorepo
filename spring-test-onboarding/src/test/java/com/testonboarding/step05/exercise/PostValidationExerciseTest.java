package com.testonboarding.step05.exercise;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;

/**
 * [Step 5 — exercise] 게시글 DTO 검증 + 예외 핸들러 테스트를 직접 작성해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example A의 MemberSignupRequestValidatorTest를 참고)
 * 3. .\gradlew :spring-test-onboarding:test 로 통과를 확인한다
 *
 * 검증 대상:
 * - PostCreateRequest의 @NotBlank / @Size(max=200) 규칙
 * - GlobalExceptionHandler의 NotPostOwnerException → 403 매핑
 *   (힌트: 핸들러도 결국 그냥 클래스다 — new 해서 메서드를 직접 호출할 수 있다!)
 */
@Disabled("과제: docs/test/education/FOR-Test-Step05.md 참고 후 @Disabled를 제거하고 완성하세요")
@DisplayName("게시글 검증 + 예외 매핑 (연습문제)")
class PostValidationExerciseTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("제목이 빈 값이면 '제목은 필수입니다' 위반 1건")
    void validate_제목빈값_필수위반() {
        // given : 제목이 "" 이고 내용은 정상인 PostCreateRequest를 만드세요
        // TODO 1

        // when : validator.validate(...)를 호출하세요
        // TODO 2

        // then : 위반이 정확히 1건이고, 메시지가 "제목은 필수입니다"인지 검증하세요
        // TODO 3 (힌트: violations.iterator().next().getMessage())
    }

    @Test
    @DisplayName("제목이 201자면 길이 위반 1건")
    void validate_제목201자_길이위반() {
        // given : 201자짜리 제목을 만드세요
        //         (힌트: Java 8에서 문자열 반복 — new String(new char[201]).replace('\0', '가'))
        // TODO 4

        // when & then : 위반 1건 + 메시지가 "제목은 200자 이하여야 합니다"인지 검증하세요
        // TODO 5
    }

    @Test
    @DisplayName("NotPostOwnerException은 403으로 매핑된다 (핸들러 직접 단위 테스트)")
    void handleNotPostOwner_403매핑() {
        // given : GlobalExceptionHandler를 new로 만들고, NotPostOwnerException(1L, "hacker")을 준비하세요
        // TODO 6

        // when : handler.handleNotPostOwner(예외)를 직접 호출하세요 (반환: ResponseEntity<ErrorResponse>)
        // TODO 7

        // then : 상태코드가 403(HttpStatus.FORBIDDEN)이고, body의 message에 "hacker"가 포함되는지 검증하세요
        // TODO 8
    }
}
