package com.testonboarding.support;

import com.testonboarding.board.domain.Post;

/**
 * [심화 Step 11] 게시글 테스트 데이터 공장 (Object Mother 패턴)
 *
 * 사용:
 *   Post post = PostFixture.post(1L, "writer1");                  // 자주 쓰는 형태
 *   Post custom = PostFixture.aPost().title("특별한 제목").build(); // 일부만 바꿔서
 */
public class PostFixture {

    private PostFixture() {
    }

    public static Post post(Long postId, String writer) {
        return aPost().postId(postId).writer(writer).build();
    }

    /** 합리적 기본값이 채워진 builder */
    public static Post.PostBuilder aPost() {
        return Post.builder()
                .postId(1L)
                .writer("writer1")
                .title("기본 제목")
                .content("기본 내용");
    }
}
