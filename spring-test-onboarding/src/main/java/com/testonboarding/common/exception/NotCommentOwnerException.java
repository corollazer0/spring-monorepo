package com.testonboarding.common.exception;

/**
 * 작성자가 아닌 사용자가 댓글을 삭제하려 할 때 발생. (403으로 변환)
 */
public class NotCommentOwnerException extends BusinessException {

    public NotCommentOwnerException(Long commentId, String requester) {
        super("댓글 작성자 본인만 삭제할 수 있습니다. commentId=" + commentId + ", 요청자=" + requester);
    }
}
