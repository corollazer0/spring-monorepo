package com.testonboarding.step09.answer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testonboarding.comment.controller.CommentController;
import com.testonboarding.comment.dto.CommentCreateRequest;
import com.testonboarding.comment.dto.CommentResponse;
import com.testonboarding.comment.service.CommentService;
import com.testonboarding.common.exception.PostNotFoundException;
import com.testonboarding.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Step 9 — answer] 댓글 Controller 테스트 (@WebMvcTest)
 *
 * 전략: HTTP 계약(URL/JSON/상태코드) + 보안 경계 + 검증 배선은 MockMvc 슬라이스로.
 * Service의 판단 로직은 CommentServiceAnswerTest가 이미 검증했으므로 @MockBean.
 */
@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
@DisplayName("댓글 컨트롤러 (캡스톤 모범답안)")
class CommentControllerAnswerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @Test
    @DisplayName("댓글 목록은 비로그인도 조회할 수 있다 (200 + JSON 배열)")
    void getComments_비로그인_200과목록() throws Exception {
        // given
        given(commentService.getComments(1L)).willReturn(Arrays.asList(
                CommentResponse.builder().commentId(1L).postId(1L).writer("writer2").content("첫 댓글").build(),
                CommentResponse.builder().commentId(2L).postId(1L).writer("writer1").content("둘째 댓글").build()));

        // when & then
        mockMvc.perform(get("/api/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content").value("첫 댓글"));
    }

    @Test
    @DisplayName("비로그인 댓글 작성 → 401")
    void createComment_미인증_401() throws Exception {
        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentCreateRequest("댓글")))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "writer1")
    @DisplayName("로그인 사용자의 댓글 작성 → 201 + Location")
    void createComment_인증사용자_201과Location() throws Exception {
        // given
        given(commentService.createComment(eq(1L), eq("writer1"), any(CommentCreateRequest.class)))
                .willReturn(7L);

        // when & then
        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentCreateRequest("좋은 글이네요")))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/comments/7"));
    }

    @Test
    @WithMockUser(username = "writer1")
    @DisplayName("빈 내용으로 댓글 작성 → 400 + fieldErrors")
    void createComment_빈내용_400과필드에러() throws Exception {
        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentCreateRequest("")))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("content"));
    }

    @Test
    @WithMockUser(username = "writer1")
    @DisplayName("없는 게시글에 댓글 작성 → 404 (advice 번역 확인)")
    void createComment_없는게시글_404() throws Exception {
        // given
        given(commentService.createComment(eq(99L), eq("writer1"), any(CommentCreateRequest.class)))
                .willThrow(new PostNotFoundException(99L));

        // when & then
        mockMvc.perform(post("/api/posts/99/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentCreateRequest("댓글")))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다. postId=99"));
    }
}
