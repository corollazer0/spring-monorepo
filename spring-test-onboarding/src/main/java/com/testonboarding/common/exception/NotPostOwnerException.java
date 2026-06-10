package com.testonboarding.common.exception;

/**
 * 작성자가 아닌 사용자가 게시글을 수정/삭제하려 할 때 발생. (Step 5에서 403으로 변환)
 *
 * 소유자 검증은 Security 설정이 아니라 Service 계층의 책임이다 —
 * "URL 접근 권한"(Security)과 "데이터 소유권"(비즈니스 규칙)은 다른 문제이기 때문.
 */
public class NotPostOwnerException extends BusinessException {

    public NotPostOwnerException(Long postId, String requester) {
        super("작성자 본인만 수정/삭제할 수 있습니다. postId=" + postId + ", 요청자=" + requester);
    }
}
