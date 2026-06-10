package com.testonboarding.step12.example;

import com.testonboarding.board.dto.PostResponse;
import com.testonboarding.board.dto.PostCreateRequest;
import com.testonboarding.board.service.BoardService;
import com.testonboarding.comment.dto.CommentResponse;
import com.testonboarding.comment.service.CommentService;
import com.testonboarding.config.SecurityConfig;
import com.testonboarding.web.PostViewController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * [Step 12 — example A] 화면(SSR) 컨트롤러 테스트: 뷰 이름, 모델, 렌더링까지
 *
 * REST 테스트(Step 4)와의 차이 — 검증 대상이 다르다:
 * - REST: 상태코드 + JSON 본문 (jsonPath)
 * - View: 뷰 이름(view().name) + 모델(model()) + 렌더링된 HTML(content())
 *
 * Thymeleaf는 MockMvc 안에서 "실제로 렌더링"된다 — 템플릿 문법 오류, 변수명 오타까지
 * 잡을 수 있다. (JSP였다면 불가능 — forward만 검증 가능. 교육 문서의 비교표 참고!)
 *
 * 그리고 보안 동작이 REST와 다르게 분기된다:
 * - REST(/api/**) 미인증 → 401
 * - 화면 미인증 → 로그인 페이지로 302 redirect  ← 같은 보안 설정, 다른 응대!
 */
@WebMvcTest(PostViewController.class)
@Import(SecurityConfig.class)
@DisplayName("게시판 화면 컨트롤러 (@WebMvcTest + Thymeleaf 렌더링)")
class PostViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @MockBean
    private CommentService commentService;

    @Nested
    @DisplayName("목록 화면 GET /posts")
    class ListView {

        @Test
        @DisplayName("뷰 이름, 모델, 렌더링된 HTML까지 3단 검증")
        void list_기본요청_뷰모델렌더링검증() throws Exception {
            // given
            given(boardService.getPosts(1, 10)).willReturn(Arrays.asList(
                    PostResponse.builder().postId(2L).writer("writer2").title("두번째 글").build(),
                    PostResponse.builder().postId(1L).writer("writer1").title("첫번째 글").build()));

            // when & then
            mockMvc.perform(get("/posts"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("post/list"))                  // (1) 올바른 템플릿 선택
                    .andExpect(model().attributeExists("posts", "page"))  // (2) 모델 데이터 전달
                    .andExpect(content().string(containsString("두번째 글")))  // (3) 실제 HTML에 찍혔는가
                    .andExpect(content().string(containsString("/posts/2"))); // 상세 링크 생성 확인
        }
    }

    @Nested
    @DisplayName("상세 화면 GET /posts/{id}")
    class DetailView {

        @Test
        @DisplayName("글과 댓글이 함께 렌더링된다")
        void detail_존재하는글_글과댓글렌더링() throws Exception {
            // given
            given(boardService.getPost(1L)).willReturn(
                    PostResponse.builder().postId(1L).writer("writer1")
                            .title("제목입니다").content("본문입니다").build());
            given(commentService.getComments(1L)).willReturn(Collections.singletonList(
                    CommentResponse.builder().commentId(1L).postId(1L)
                            .writer("writer2").content("좋은 글이네요").build()));

            // when & then
            mockMvc.perform(get("/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("post/detail"))
                    .andExpect(content().string(containsString("제목입니다")))
                    .andExpect(content().string(containsString("좋은 글이네요"))); // 댓글까지!
        }
    }

    @Nested
    @DisplayName("글쓰기 — 보안 분기와 폼 흐름")
    class CreateFlow {

        /**
         * Step 4의 createPost_미인증_401 과 비교해보라!
         * 같은 SecurityConfig인데 화면(/posts/new)은 401이 아니라 로그인 페이지로 302다.
         * EntryPoint를 /api/**(401)와 그 외(로그인 redirect)로 분기했기 때문 —
         * "프로그램에겐 상태코드를, 사람에겐 안내를".
         *
         * ⚠️ accept(TEXT_HTML)이 필수다! 브라우저는 항상 Accept: text/html을 보내지만
         * MockMvc의 기본값은 "모든 타입 허용"이라 브라우저로 인식되지 않는다.
         * 로그인 redirect EntryPoint는 HTML을 원하는 요청에만 적용되기 때문 —
         * accept를 빼고 실행해보면 401이 나온다(직접 해보라!).
         */
        @Test
        @DisplayName("미인증 글쓰기 화면 → 401이 아니라 로그인 페이지로 redirect")
        void createForm_미인증_로그인으로redirect() throws Exception {
            mockMvc.perform(get("/posts/new")
                            .accept(MediaType.TEXT_HTML)) // 브라우저인 척!
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @WithMockUser(username = "writer1")
        @DisplayName("인증하면 빈 폼 객체와 함께 작성 화면이 열린다")
        void createForm_인증_폼화면() throws Exception {
            mockMvc.perform(get("/posts/new"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("post/form"))
                    .andExpect(model().attributeExists("postCreateRequest"));
        }

        /**
         * 폼 제출은 JSON이 아니라 폼 파라미터(param)로 보낸다 — @ModelAttribute 바인딩.
         * 성공 응답은 200이 아니라 redirect (PRG 패턴: 새로고침 중복 제출 방지).
         */
        @Test
        @WithMockUser(username = "writer1")
        @DisplayName("폼 제출 성공 → 상세 화면으로 redirect (PRG 패턴)")
        void create_정상제출_상세로redirect() throws Exception {
            // given
            given(boardService.createPost(eq("writer1"), any(PostCreateRequest.class)))
                    .willReturn(10L);

            // when & then
            mockMvc.perform(post("/posts")
                            .param("title", "화면에서 쓴 글")
                            .param("content", "폼 파라미터로 바인딩된다")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/posts/10"));
        }

        /**
         * REST에서는 검증 실패가 400 JSON이었다(Step 5).
         * 화면에서는 "같은 폼을 에러와 함께 재표시" — 사람은 다시 입력해야 하니까.
         * BindingResult가 @Valid 바로 뒤에 있어야 이 흐름이 가능하다.
         */
        @Test
        @WithMockUser(username = "writer1")
        @DisplayName("검증 실패 → 400 대신 폼 재표시 + 필드 에러 + 서비스 미호출")
        void create_제목없음_폼재표시와에러() throws Exception {
            // when & then
            mockMvc.perform(post("/posts")
                            .param("title", "")           // @NotBlank 위반
                            .param("content", "내용만 있음")
                            .with(csrf()))
                    .andExpect(status().isOk())           // 에러여도 200 — 폼을 다시 보여주는 것
                    .andExpect(view().name("post/form"))
                    .andExpect(model().attributeHasFieldErrors("postCreateRequest", "title"))
                    .andExpect(content().string(containsString("제목은 필수입니다"))); // 에러 메시지 렌더링

            // 문 앞에서 걸렀다는 증명 (Step 5와 같은 철학)
            then(boardService).should(never()).createPost(any(), any());
        }
    }
}
