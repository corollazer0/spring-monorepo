package com.testonboarding.board.dao;

import com.testonboarding.board.domain.Post;
import com.testonboarding.board.dto.PostSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 게시글 DAO — 구현은 resources/mybatis/mapper/BoardMapper.xml
 *
 * @Mapper: MyBatis가 이 인터페이스의 프록시 구현체를 만들어 Bean으로 등록한다.
 * 메서드명과 XML의 statement id가 1:1로 연결된다.
 */
@Mapper
public interface BoardDao {

    Post findById(Long postId);

    /**
     * 페이징 조회 — MS-SQL 스타일 OFFSET/FETCH 사용 (BoardMapper.xml 참고)
     */
    List<Post> findPage(@Param("offset") int offset, @Param("size") int size);

    /**
     * 동적 검색 — 조건이 null이면 해당 조건은 SQL에서 빠진다
     */
    List<Post> search(PostSearchCondition condition);

    long count();

    void insert(Post post);

    int update(Post post);

    int deleteById(Long postId);
}
