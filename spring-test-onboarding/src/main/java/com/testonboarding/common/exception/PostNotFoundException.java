package com.testonboarding.common.exception;

/**
 * 존재하지 않는 게시글 조회 시 발생. (Step 5에서 404로 변환)
 */
public class PostNotFoundException extends BusinessException {

    public PostNotFoundException(Long postId) {
        super("게시글을 찾을 수 없습니다. postId=" + postId);
    }
}
