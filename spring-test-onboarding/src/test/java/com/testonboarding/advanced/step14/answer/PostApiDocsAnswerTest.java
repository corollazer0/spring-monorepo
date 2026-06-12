package com.testonboarding.advanced.step14.answer;

import com.testonboarding.board.controller.BoardController;
import com.testonboarding.board.dto.PostResponse;
import com.testonboarding.board.service.BoardService;
import com.testonboarding.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [심화 Step 14 — answer] PostApiDocsExerciseTest 모범답안
 *
 * 채점 포인트: RestDocumentationRequestBuilders.get을 썼는가 —
 * MockMvcRequestBuilders.get으로는 pathParameters가 "urlTemplate not found"로
 * 실패한다 (URL 템플릿 보존 여부의 차이, 이 Step의 1번 함정).
 */
@WebMvcTest(BoardController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs
@DisplayName("게시글 API 문서화 (모범답안)")
class PostApiDocsAnswerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @Test
    @DisplayName("단건 조회 문서화: 경로 변수 + 응답 필드 5종")
    void getPost_문서화() throws Exception {
        // given (TODO 1 답) : null 필드 없이 전부 채운다 — 문서와 페이로드의 1:1 대응
        given(boardService.getPost(1L)).willReturn(PostResponse.builder()
                .postId(1L).writer("writer1").title("첫 번째 글")
                .content("내용입니다").createdAt(LocalDateTime.of(2026, 6, 12, 10, 0))
                .build());

        // when & then (TODO 2 답) : RestDocumentationRequestBuilders.get — URL 템플릿 보존!
        mockMvc.perform(get("/api/posts/{postId}", 1L))
                .andExpect(status().isOk())
                .andDo(document("post-get",
                        pathParameters(
                                parameterWithName("postId").description("조회할 게시글 ID")),
                        responseFields(
                                fieldWithPath("postId").description("게시글 ID"),
                                fieldWithPath("writer").description("작성자 아이디"),
                                fieldWithPath("title").description("제목"),
                                fieldWithPath("content").description("본문"),
                                fieldWithPath("createdAt").description("작성 시각 (ISO-8601)"))));

        // then (TODO 3 답) : 경로 변수 문서 조각이 실제로 생겼다
        assertThat(Files.exists(Paths.get(
                "build", "generated-snippets", "post-get", "path-parameters.adoc"))).isTrue();
    }
}
