package com.testonboarding.board.dao;

import com.testonboarding.board.domain.Post;

/**
 * 게시글 DAO 인터페이스.
 *
 * Step 2 시점에는 구현(XML)이 아직 없다 — 그래서 Service 테스트에서 Mock으로 대체한다.
 * Step 3에서 @Mapper + BoardMapper.xml 로 실제 구현이 붙는다.
 */
public interface BoardDao {

    Post findById(Long postId);

    void insert(Post post);

    int update(Post post);

    int deleteById(Long postId);
}
