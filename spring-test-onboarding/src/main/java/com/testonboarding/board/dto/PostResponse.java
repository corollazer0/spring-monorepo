package com.testonboarding.board.dto;

import com.testonboarding.board.domain.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 게시글 응답 DTO — 도메인을 그대로 노출하지 않고 응답 전용 형태로 변환한다.
 */
@Getter
@Builder
public class PostResponse {

    private final Long postId;
    private final String writer;
    private final String title;
    private final String content;
    private final LocalDateTime createdAt;

    public static PostResponse from(Post post) {
        return PostResponse.builder()
                .postId(post.getPostId())
                .writer(post.getWriter())
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
