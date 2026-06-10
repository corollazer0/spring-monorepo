package com.testonboarding.comment.service;

import com.testonboarding.board.dao.BoardDao;
import com.testonboarding.comment.dao.CommentDao;
import com.testonboarding.comment.domain.Comment;
import com.testonboarding.comment.dto.CommentCreateRequest;
import com.testonboarding.comment.dto.CommentResponse;
import com.testonboarding.common.exception.CommentNotFoundException;
import com.testonboarding.common.exception.NotCommentOwnerException;
import com.testonboarding.common.exception.PostNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 댓글 서비스 — Step 9 캡스톤 대상.
 *
 * 비즈니스 규칙 (FOR-Test-Step09-Requirements.md):
 * 1. 없는 게시글에는 댓글을 달 수 없다 → PostNotFoundException
 * 2. 댓글 작성자 본인만 삭제할 수 있다 → NotCommentOwnerException
 * 3. 없는 댓글 삭제 시도 → CommentNotFoundException
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentDao commentDao;
    private final BoardDao boardDao;

    public List<CommentResponse> getComments(Long postId) {
        return commentDao.findByPostId(postId).stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    public Long createComment(Long postId, String writerUsername, CommentCreateRequest request) {
        if (boardDao.findById(postId) == null) {
            throw new PostNotFoundException(postId);
        }

        Comment comment = Comment.builder()
                .postId(postId)
                .writer(writerUsername)
                .content(request.getContent())
                .build();

        commentDao.insert(comment);
        log.info(">>>>> [CommentService] 댓글 작성 완료. commentId={}, postId={}",
                comment.getCommentId(), postId);
        return comment.getCommentId();
    }

    public void deleteComment(Long commentId, String requesterUsername) {
        Comment comment = commentDao.findById(commentId);
        if (comment == null) {
            throw new CommentNotFoundException(commentId);
        }
        if (!comment.getWriter().equals(requesterUsername)) {
            throw new NotCommentOwnerException(commentId, requesterUsername);
        }

        commentDao.deleteById(commentId);
        log.info(">>>>> [CommentService] 댓글 삭제 완료. commentId={}", commentId);
    }
}
