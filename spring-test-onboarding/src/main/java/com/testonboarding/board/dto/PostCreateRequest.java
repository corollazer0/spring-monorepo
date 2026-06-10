package com.testonboarding.board.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게시글 작성 요청 DTO. (검증 어노테이션은 Step 5에서 추가)
 */
@Getter
@Setter
@NoArgsConstructor
public class PostCreateRequest {

    private String title;
    private String content;

    public PostCreateRequest(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
