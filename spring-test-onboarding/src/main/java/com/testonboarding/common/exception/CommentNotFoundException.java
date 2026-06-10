package com.testonboarding.common.exception;

/**
 * 존재하지 않는 댓글 접근 시 발생. (404로 변환)
 */
public class CommentNotFoundException extends BusinessException {

    public CommentNotFoundException(Long commentId) {
        super("댓글을 찾을 수 없습니다. commentId=" + commentId);
    }
}
