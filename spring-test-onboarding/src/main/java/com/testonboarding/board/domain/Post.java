package com.testonboarding.board.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 게시글 도메인.
 *
 * - postId: DB의 IDENTITY가 채번 (MyBatis useGeneratedKeys로 insert 후 채워짐 — Step 3)
 * - writer: 작성자 username — 수정/삭제 시 소유자 검증의 기준
 * - @Setter는 MyBatis 결과 매핑과 useGeneratedKeys(keyProperty)에 필요
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    private Long postId;
    private String writer;
    private String title;
    private String content;
    private LocalDateTime createdAt;

    /**
     * 제목/내용 수정 — 무엇이 변경 가능한지를 도메인 메서드로 표현한다.
     */
    public void change(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
