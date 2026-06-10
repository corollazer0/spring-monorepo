package com.testonboarding.board.service;

import com.testonboarding.board.dao.BoardDao;
import com.testonboarding.board.domain.Post;
import com.testonboarding.board.dto.PostCreateRequest;
import com.testonboarding.board.dto.PostResponse;
import com.testonboarding.board.dto.PostUpdateRequest;
import com.testonboarding.common.exception.NotPostOwnerException;
import com.testonboarding.common.exception.PostNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 게시판 서비스 — Step 2 테스트 대상.
 *
 * 핵심 비즈니스 규칙:
 * 1. 없는 글 조회/수정/삭제 → PostNotFoundException
 * 2. 작성자 본인만 수정/삭제 가능 → 위반 시 NotPostOwnerException
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardDao boardDao;

    public PostResponse getPost(Long postId) {
        Post post = findPostOrThrow(postId);
        return PostResponse.from(post);
    }

    /**
     * 페이징 목록 조회 — page는 1부터 시작.
     */
    public List<PostResponse> getPosts(int page, int size) {
        int offset = (page - 1) * size;
        return boardDao.findPage(offset, size).stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());
    }

    public Long createPost(String writerUsername, PostCreateRequest request) {
        Post post = Post.builder()
                .writer(writerUsername)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        boardDao.insert(post); // useGeneratedKeys로 postId가 채워진다 (Step 3)
        log.info(">>>>> [BoardService] 게시글 생성 완료. postId={}, writer={}", post.getPostId(), writerUsername);
        return post.getPostId();
    }

    public void updatePost(Long postId, String requesterUsername, PostUpdateRequest request) {
        Post post = findPostOrThrow(postId);
        validateOwner(post, requesterUsername);

        post.change(request.getTitle(), request.getContent());
        boardDao.update(post);
        log.info(">>>>> [BoardService] 게시글 수정 완료. postId={}", postId);
    }

    public void deletePost(Long postId, String requesterUsername) {
        Post post = findPostOrThrow(postId);
        validateOwner(post, requesterUsername);

        boardDao.deleteById(postId);
        log.info(">>>>> [BoardService] 게시글 삭제 완료. postId={}", postId);
    }

    private Post findPostOrThrow(Long postId) {
        Post post = boardDao.findById(postId);
        if (post == null) {
            throw new PostNotFoundException(postId);
        }
        return post;
    }

    private void validateOwner(Post post, String requesterUsername) {
        if (!post.getWriter().equals(requesterUsername)) {
            throw new NotPostOwnerException(post.getPostId(), requesterUsername);
        }
    }
}
