package com.testonboarding.comment.controller;

import com.testonboarding.comment.dto.CommentCreateRequest;
import com.testonboarding.comment.dto.CommentResponse;
import com.testonboarding.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;

/**
 * 댓글 REST API — Step 9 캡스톤 대상.
 *
 * 보안 정책 (SecurityConfig):
 * - GET  /api/posts/{postId}/comments : 누구나 (GET /api/posts/** permitAll에 포함)
 * - POST /api/posts/{postId}/comments : 인증 필요
 * - DELETE /api/comments/{commentId}  : 인증 필요
 */
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/api/posts/{postId}/comments")
    public List<CommentResponse> getComments(@PathVariable Long postId) {
        return commentService.getComments(postId);
    }

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<Void> createComment(@PathVariable Long postId,
                                              @Valid @RequestBody CommentCreateRequest request,
                                              Principal principal) {
        Long commentId = commentService.createComment(postId, principal.getName(), request);
        return ResponseEntity.created(URI.create("/api/comments/" + commentId)).build();
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId,
                                              Principal principal) {
        commentService.deleteComment(commentId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
