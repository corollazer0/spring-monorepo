package com.testonboarding.comment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 댓글 도메인 — Step 9 캡스톤 대상.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    private Long commentId;
    private Long postId;
    private String writer;
    private String content;
    private LocalDateTime createdAt;
}
