package com.testonboarding.board.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 게시글 작성 요청 DTO.
 *
 * Bean Validation(Step 5): 형식 검증은 DTO가 선언적으로 —
 * "필수인가, 길이는?" 같은 형식 규칙은 비즈니스 로직(Service)까지 가기 전에 문 앞에서 거른다.
 */
@Getter
@Setter
@NoArgsConstructor
public class PostCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 2000, message = "내용은 2000자 이하여야 합니다")
    private String content;

    public PostCreateRequest(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
