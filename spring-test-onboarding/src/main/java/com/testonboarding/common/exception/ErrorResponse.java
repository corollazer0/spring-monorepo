package com.testonboarding.common.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 에러 응답 규약 — 모든 에러는 이 형태의 JSON으로 나간다.
 *
 * {
 *   "status": 400,
 *   "message": "입력값 검증에 실패했습니다",
 *   "fieldErrors": [ { "field": "title", "reason": "제목은 필수입니다" } ]
 * }
 *
 * 응답 포맷이 일관되어야 프론트엔드가 에러 처리를 한 곳에서 할 수 있다.
 */
@Getter
@Builder
public class ErrorResponse {

    private final int status;
    private final String message;

    @Builder.Default
    private final List<FieldErrorDetail> fieldErrors = new ArrayList<>();

    public static ErrorResponse of(HttpStatus status, String message) {
        return ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .build();
    }

    /**
     * Bean Validation 실패용 — 어떤 필드가 왜 거절됐는지 목록으로 담는다.
     */
    public static ErrorResponse of(HttpStatus status, String message, BindingResult bindingResult) {
        List<FieldErrorDetail> details = bindingResult.getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        return ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .fieldErrors(details)
                .build();
    }

    @Getter
    @AllArgsConstructor
    public static class FieldErrorDetail {
        private final String field;
        private final String reason;
    }
}
