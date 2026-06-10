package com.testonboarding.comment.dao;

import com.testonboarding.comment.domain.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 댓글 DAO — 구현은 resources/mybatis/mapper/CommentMapper.xml
 */
@Mapper
public interface CommentDao {

    Comment findById(Long commentId);

    /** 게시글의 댓글을 등록순(comment_id ASC)으로 */
    List<Comment> findByPostId(Long postId);

    void insert(Comment comment);

    int deleteById(Long commentId);
}
