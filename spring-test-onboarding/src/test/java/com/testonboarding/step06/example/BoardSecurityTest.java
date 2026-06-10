package com.testonboarding.step06.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testonboarding.board.controller.BoardController;
import com.testonboarding.board.dto.PostCreateRequest;
import com.testonboarding.board.dto.PostUpdateRequest;
import com.testonboarding.board.service.BoardService;
import com.testonboarding.common.exception.NotPostOwnerException;
import com.testonboarding.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Step 6 — example] Security 테스트: 인증(Authentication)과 인가(Authorization)
 *
 * 문제 상황: "로그인한 사용자만 글을 쓸 수 있다"를 테스트하려면
 * 매번 진짜로 회원가입 + 로그인해서 세션을 받아야 하나? 너무 무겁다.
 *
 * 해결: spring-security-test
 * - @WithMockUser: "이미 로그인된 상태"를 SecurityContext에 직접 심어준다 (가장 간단)
 * - user(...): 요청 단위로 인증 사용자를 지정하는 RequestPostProcessor
 * - csrf(): 유효한 CSRF 토큰을 요청에 실어준다
 *
 * 📖 핵심 용어 — 401 vs 403:
 * - 401 Unauthorized : "너 누구야?"   — 인증(로그인) 자체가 없음
 * - 403 Forbidden    : "너 안 돼"     — 로그인은 했지만 권한/조건이 안 됨
 * 이 구분이 흐려지면 장애 분석이 산으로 간다. 테스트로 정확히 박아두자.
 */
@WebMvcTest(BoardController.class)
@Import(SecurityConfig.class)
@DisplayName("게시판 보안 (인증/인가)")
class BoardSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BoardService boardService;

    @Nested
    @DisplayName("인증(Authentication) — 너 누구야?")
    class AuthenticationCases {

        @Test
        @WithAnonymousUser // 익명 사용자임을 명시 (안 붙여도 익명이지만, 의도를 드러낸다)
        @DisplayName("미인증 글쓰기 → 401 (Controller에 도달하지 못한다)")
        void createPost_미인증_401() throws Exception {
            // given
            String body = objectMapper.writeValueAsString(new PostCreateRequest("제목", "내용"));

            // when & then
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "writer1")
        @DisplayName("인증된 사용자의 글쓰기 → 201, principal이 Service까지 흘러간다")
        void createPost_인증사용자_201() throws Exception {
            // given : Service는 Mock — 여기서 검증하는 건 "인증 통과 + username 전달"이다
            given(boardService.createPost(eq("writer1"), any(PostCreateRequest.class)))
                    .willReturn(10L);
            String body = objectMapper.writeValueAsString(new PostCreateRequest("제목", "내용"));

            // when & then : eq("writer1") stubbing이 적중해 10L이 반환됐다는 것 자체가
            //               "@WithMockUser의 username이 Principal로 Controller에 주입되어
            //                Service까지 정확히 전달됐다"는 증명이다
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/posts/10"));
        }

        /**
         * @WithMockUser 대신 요청 단위로 사용자를 지정하는 방법 — user() RequestPostProcessor.
         * 한 테스트 안에서 여러 사용자를 번갈아 흉내낼 때 유용하다.
         */
        @Test
        @DisplayName("user() 방식으로도 인증 상태를 만들 수 있다")
        void createPost_user후처리기_201() throws Exception {
            // given
            given(boardService.createPost(eq("writer2"), any(PostCreateRequest.class)))
                    .willReturn(11L);
            String body = objectMapper.writeValueAsString(new PostCreateRequest("제목", "내용"));

            // when & then
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .with(user("writer2"))   // 이 요청만 writer2로 인증된 상태
                            .with(csrf()))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("CSRF — 정체불명이었던 with(csrf())의 정체")
    class CsrfCases {

        /**
         * 세션 기반 인증에서 상태 변경 요청(POST/PUT/DELETE)은 CSRF 토큰이 필수다.
         * (악성 사이트가 내 브라우저의 세션 쿠키를 이용해 몰래 요청을 보내는 공격 방어)
         *
         * 주의: 인증된 사용자여도 토큰이 없으면 403 — 401이 아니다!
         * CSRF 검증은 인증 확인보다 먼저 수행되기 때문에, 토큰 누락 시
         * "로그인했는데 왜 403?!"이라는 단골 미스터리가 생긴다.
         */
        @Test
        @WithMockUser(username = "writer1")
        @DisplayName("인증했어도 CSRF 토큰이 없으면 403")
        void createPost_csrf토큰없음_403() throws Exception {
            // given
            String body = objectMapper.writeValueAsString(new PostCreateRequest("제목", "내용"));

            // when & then : .with(csrf())를 일부러 뺐다
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET(읽기)은 CSRF와 무관하다 — 상태를 바꾸지 않으므로")
        void getPosts_GET요청_csrf불필요() throws Exception {
            mockMvc.perform(get("/api/posts"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("인가(Authorization) — 너 안 돼")
    class AuthorizationCases {

        /**
         * URL 인가: SecurityConfig의 hasRole(ADMIN) 검증.
         * 일반 USER가 /api/admin/** 에 접근하면 핸들러 존재 여부와 무관하게 403.
         */
        @Test
        @WithMockUser(username = "writer1", roles = "USER")
        @DisplayName("일반 USER가 admin URL 접근 → 403 (URL 단위 인가)")
        void adminUrl_일반사용자_403() throws Exception {
            mockMvc.perform(get("/api/admin/dashboard"))
                    .andExpect(status().isForbidden());
        }

        /**
         * 데이터 소유권 인가: 작성자 본인만 수정 가능.
         * 이건 SecurityConfig(URL)가 아니라 Service(Step 2)의 책임이고,
         * GlobalExceptionHandler(Step 5)가 403으로 번역한다.
         * → Security + Service + Advice 세 층의 합작을 한 번에 검증하는 테스트.
         */
        @Test
        @WithMockUser(username = "hacker")
        @DisplayName("타인 글 수정 시도 → 403 (데이터 소유권 인가)")
        void updatePost_타인글_403() throws Exception {
            // given
            willThrow(new NotPostOwnerException(1L, "hacker"))
                    .given(boardService).updatePost(eq(1L), eq("hacker"), any(PostUpdateRequest.class));
            String body = objectMapper.writeValueAsString(new PostUpdateRequest("탈취", "탈취"));

            // when & then
            mockMvc.perform(put("/api/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            "작성자 본인만 수정/삭제할 수 있습니다. postId=1, 요청자=hacker"));
        }

        @Test
        @WithMockUser(username = "writer1")
        @DisplayName("본인 글 수정 → 200")
        void updatePost_본인글_200() throws Exception {
            // given : Service가 예외 없이 통과하는 시나리오 (기본 Mock 동작 = 아무 일 없음)
            String body = objectMapper.writeValueAsString(new PostUpdateRequest("새 제목", "새 내용"));

            // when & then
            mockMvc.perform(put("/api/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .with(csrf()))
                    .andExpect(status().isOk());
        }
    }
}
