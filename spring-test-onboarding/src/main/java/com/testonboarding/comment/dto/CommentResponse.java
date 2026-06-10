package com.testonboarding.comment.dto;

import com.testonboarding.comment.domain.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 댓글 응답 DTO.
 */
@Getter
@Builder
public class CommentResponse {

    private final Long commentId;
    private final Long postId;
    private final String writer;
    private final String content;
    private final LocalDateTime createdAt;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPostId())
                .writer(comment.getWriter())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
