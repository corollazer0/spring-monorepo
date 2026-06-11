package com.webflow.common.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 에러 응답 규약 — 모든 에러는 이 형태의 JSON으로 나간다 (TestCraft와 동일 규약).
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
