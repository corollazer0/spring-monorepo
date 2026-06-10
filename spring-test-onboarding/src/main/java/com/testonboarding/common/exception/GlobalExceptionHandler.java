package com.testonboarding.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기 — "예외 → HTTP 응답" 번역을 한 곳에 모은다.
 *
 * @RestControllerAdvice: 모든 @RestController에서 던져진 예외를 여기서 가로챈다.
 * 더 구체적인 예외 타입의 핸들러가 우선한다 (선언 순서와 무관).
 *
 * 예외 → 상태코드 매핑 규약:
 * - MethodArgumentNotValidException (@Valid 실패)  → 400 + 필드별 사유
 * - IllegalArgumentException (정책 위반 등)         → 400
 * - PostNotFoundException                          → 404
 * - NotPostOwnerException                          → 403
 * - DuplicateUsernameException                     → 409
 * - 그 외 BusinessException                         → 400
 * - 그 외 모든 예외                                  → 500 (내부 정보는 숨긴다!)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** @Valid 검증 실패 — 어떤 필드가 왜 거절됐는지 응답에 담는다 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn(">>>>> [WARN] 입력값 검증 실패: {}건", e.getBindingResult().getErrorCount());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다", e.getBindingResult());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePostNotFound(PostNotFoundException e) {
        log.warn(">>>>> [WARN] 게시글 없음: {}", e.getMessage());
        return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NotPostOwnerException.class)
    public ResponseEntity<ErrorResponse> handleNotPostOwner(NotPostOwnerException e) {
        log.warn(">>>>> [WARN] 소유자 아님: {}", e.getMessage());
        return errorResponse(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFound(CommentNotFoundException e) {
        log.warn(">>>>> [WARN] 댓글 없음: {}", e.getMessage());
        return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NotCommentOwnerException.class)
    public ResponseEntity<ErrorResponse> handleNotCommentOwner(NotCommentOwnerException e) {
        log.warn(">>>>> [WARN] 댓글 소유자 아님: {}", e.getMessage());
        return errorResponse(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUsername(DuplicateUsernameException e) {
        log.warn(">>>>> [WARN] 중복 아이디: {}", e.getMessage());
        return errorResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    /** 위의 구체적 핸들러에 안 걸린 비즈니스 예외의 기본값 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        log.warn(">>>>> [WARN] 비즈니스 규칙 위반: {}", e.getMessage());
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** 비밀번호 정책(Step 1) 등 순수 자바 검증기가 던지는 예외 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn(">>>>> [WARN] 잘못된 요청: {}", e.getMessage());
        return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * 최후의 보루 — 예상 못 한 예외.
     * 스택트레이스/내부 메시지를 절대 응답에 담지 않는다 (보안!) — 로그에만 남긴다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error(">>>>> [ERROR] 예상치 못한 오류: {}", e.getMessage(), e);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");
    }

    private ResponseEntity<ErrorResponse> errorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message));
    }
}
