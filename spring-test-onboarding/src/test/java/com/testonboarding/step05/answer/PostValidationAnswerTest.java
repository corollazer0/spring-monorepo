package com.testonboarding.step05.answer;

import com.testonboarding.board.dto.PostCreateRequest;
import com.testonboarding.common.exception.ErrorResponse;
import com.testonboarding.common.exception.GlobalExceptionHandler;
import com.testonboarding.common.exception.NotPostOwnerException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Step 5 — answer] PostValidationExerciseTest 모범답안
 *
 * 채점 포인트:
 * - 위반 "건수"와 "메시지" 둘 다 검증했는가
 * - 핸들러를 웹 없이 직접 단위 테스트할 수 있음을 이해했는가
 *   (@RestControllerAdvice도 결국 평범한 클래스다)
 */
@DisplayName("게시글 검증 + 예외 매핑 (모범답안)")
class PostValidationAnswerTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("제목이 빈 값이면 '제목은 필수입니다' 위반 1건")
    void validate_제목빈값_필수위반() {
        // given (TODO 1 답)
        PostCreateRequest request = new PostCreateRequest("", "정상 내용");

        // when (TODO 2 답)
        Set<ConstraintViolation<PostCreateRequest>> violations = validator.validate(request);

        // then (TODO 3 답)
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("제목은 필수입니다");
    }

    @Test
    @DisplayName("제목이 201자면 길이 위반 1건")
    void validate_제목201자_길이위반() {
        // given (TODO 4 답) : Java 8식 문자열 반복
        String longTitle = new String(new char[201]).replace('\0', '가');
        PostCreateRequest request = new PostCreateRequest(longTitle, "정상 내용");

        // when & then (TODO 5 답)
        Set<ConstraintViolation<PostCreateRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("제목은 200자 이하여야 합니다");
    }

    @Test
    @DisplayName("NotPostOwnerException은 403으로 매핑된다 (핸들러 직접 단위 테스트)")
    void handleNotPostOwner_403매핑() {
        // given (TODO 6 답) : 핸들러도 그냥 클래스 — Spring 없이 new
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        NotPostOwnerException exception = new NotPostOwnerException(1L, "hacker");

        // when (TODO 7 답)
        ResponseEntity<ErrorResponse> response = handler.handleNotPostOwner(exception);

        // then (TODO 8 답)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).contains("hacker");
    }
}
