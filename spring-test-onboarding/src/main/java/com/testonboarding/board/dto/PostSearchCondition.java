package com.testonboarding.board.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게시글 검색 조건 — MyBatis 동적 SQL(<if>)의 파라미터.
 * null이거나 빈 값인 조건은 SQL에서 제외된다 (BoardMapper.xml의 <where> + <if> 참고).
 */
@Getter
@Setter
@NoArgsConstructor
public class PostSearchCondition {

    private String title;   // 제목 부분 일치
    private String writer;  // 작성자 정확히 일치

    public PostSearchCondition(String title, String writer) {
        this.title = title;
        this.writer = writer;
    }
}
