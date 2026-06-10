package com.testonboarding.comment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 댓글 작성 요청 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class CommentCreateRequest {

    @NotBlank(message = "댓글 내용은 필수입니다")
    @Size(max = 500, message = "댓글은 500자 이하여야 합니다")
    private String content;

    public CommentCreateRequest(String content) {
        this.content = content;
    }
}
