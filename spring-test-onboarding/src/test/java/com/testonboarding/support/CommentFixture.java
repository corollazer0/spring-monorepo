package com.testonboarding.support;

import com.testonboarding.comment.domain.Comment;

/**
 * [심화 Step 11 — answer 지원] 댓글 테스트 데이터 공장.
 * (exercise의 모범답안용 — 직접 만든 뒤 비교해보세요)
 */
public class CommentFixture {

    private CommentFixture() {
    }

    public static Comment comment(Long commentId, String writer) {
        return aComment().commentId(commentId).writer(writer).build();
    }

    public static Comment.CommentBuilder aComment() {
        return Comment.builder()
                .commentId(1L)
                .postId(1L)
                .writer("writer1")
                .content("기본 댓글");
    }
}
