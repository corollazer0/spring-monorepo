package com.testonboarding.board.controller;

import com.testonboarding.board.dto.PostCreateRequest;
import com.testonboarding.board.dto.PostResponse;
import com.testonboarding.board.dto.PostUpdateRequest;
import com.testonboarding.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;

/**
 * 게시판 REST API — Step 4 테스트 대상.
 *
 * Controller의 관심사는 HTTP다:
 * URL 매핑, 파라미터 바인딩, JSON 직렬화, 상태코드, 인증 주체 꺼내기.
 * 비즈니스 판단(없는 글? 작성자 본인?)은 전부 Service에 위임한다.
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public List<PostResponse> getPosts(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return boardService.getPosts(page, size);
    }

    @GetMapping("/{postId}")
    public PostResponse getPost(@PathVariable Long postId) {
        return boardService.getPost(postId);
    }

    /**
     * Principal: Spring MVC가 현재 로그인한 사용자를 주입해준다.
     * 미인증 요청은 Security가 여기 오기 전에 401로 차단한다(SecurityConfig).
     */
    @PostMapping
    public ResponseEntity<Void> createPost(@Valid @RequestBody PostCreateRequest request,
                                           Principal principal) {
        Long postId = boardService.createPost(principal.getName(), request);
        return ResponseEntity.created(URI.create("/api/posts/" + postId)).build();
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(@PathVariable Long postId,
                                           @Valid @RequestBody PostUpdateRequest request,
                                           Principal principal) {
        boardService.updatePost(postId, principal.getName(), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           Principal principal) {
        boardService.deletePost(postId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
