package com.testonboarding.step04.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testonboarding.board.controller.BoardController;
import com.testonboarding.board.dto.PostCreateRequest;
import com.testonboarding.board.dto.PostResponse;
import com.testonboarding.board.service.BoardService;
import com.testonboarding.common.exception.PostNotFoundException;
import com.testonboarding.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Step 4 — example] Controller 테스트: @WebMvcTest + MockMvc
 *
 * 문제 상황: Service(Step 2)도 DAO(Step 3)도 검증됐다. 하지만
 * "GET /api/posts/1 이라는 HTTP 요청이 200과 올바른 JSON으로 응답되는가"는 별개의 세계다.
 * URL 매핑 오타, 파라미터 바인딩 실수, JSON 필드명 변경 — 전부 여기서 터진다.
 *
 * 해결: @WebMvcTest 슬라이스
 * - MVC 레이어(Controller, Jackson, Security 필터)만 띄운다 — Service/DAO는 안 뜬다
 * - 안 뜨는 Service는 @MockBean으로 채워 넣는다 (Step 2의 Mock과 같은 개념,
 *   단 "Spring 컨테이너 안의 Bean을 Mock으로 교체"한다는 점이 다르다)
 * - MockMvc: 실제 서버(Tomcat) 없이 DispatcherServlet에 가짜 HTTP 요청을 넣는 도구
 *
 * ⚠️ @Import(SecurityConfig.class):
 * 이게 없으면 Spring Boot의 "기본 보안 설정"(모든 요청 인증 필요)이 적용되어
 * permitAll이어야 할 GET조차 401이 떨어진다. Lessons Learned 참고.
 */
@WebMvcTest(BoardController.class)
@Import(SecurityConfig.class)
@DisplayName("게시판 컨트롤러 (@WebMvcTest)")
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // 요청 본문 JSON 생성용

    /** 진짜 BoardService 대신 컨테이너에 등록되는 Mock — HTTP 검증에 집중하기 위해 */
    @MockBean
    private BoardService boardService;

    @Nested
    @DisplayName("목록 조회 GET /api/posts")
    class GetPosts {

        @Test
        @DisplayName("200과 함께 JSON 배열을 반환한다")
        void getPosts_기본요청_200과JSON목록() throws Exception {
            // given
            given(boardService.getPosts(1, 10)).willReturn(Arrays.asList(
                    PostResponse.builder().postId(2L).writer("writer2").title("두번째 글").build(),
                    PostResponse.builder().postId(1L).writer("writer1").title("첫번째 글").build()
            ));

            // when & then : 요청 → 상태코드 → JSON 본문까지 한 체인으로 검증
            mockMvc.perform(get("/api/posts"))
                    .andDo(print()) // 요청/응답 전문을 콘솔에 출력 — 디버깅의 시작점
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].postId").value(2))
                    .andExpect(jsonPath("$[0].title").value("두번째 글"))
                    .andExpect(jsonPath("$[1].writer").value("writer1"));
        }

        @Test
        @DisplayName("page/size 쿼리 파라미터가 서비스에 그대로 전달된다")
        void getPosts_페이지파라미터_서비스에전달() throws Exception {
            // when
            mockMvc.perform(get("/api/posts")
                            .param("page", "2")
                            .param("size", "5"))
                    .andExpect(status().isOk());

            // then : 바인딩 검증 — "?page=2&size=5"가 int 2, 5로 변환되어 도착했는가
            verify(boardService).getPosts(2, 5);
        }
    }

    @Nested
    @DisplayName("단건 조회 GET /api/posts/{postId}")
    class GetPost {

        @Test
        @DisplayName("존재하는 글이면 200과 필드가 채워진 JSON")
        void getPost_존재하는글_200과JSON() throws Exception {
            // given
            given(boardService.getPost(1L)).willReturn(
                    PostResponse.builder()
                            .postId(1L).writer("writer1")
                            .title("제목").content("내용")
                            .build());

            // when & then : URL 경로변수 {postId}가 Long 1L로 바인딩되어 서비스로 전달됐고,
            //               응답 JSON의 필드명이 DTO의 getter와 일치하는지까지 검증된다
            mockMvc.perform(get("/api/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.postId").value(1))
                    .andExpect(jsonPath("$.title").value("제목"))
                    .andExpect(jsonPath("$.content").value("내용"));
        }

        /**
         * 🚨 cliffhanger: 없는 글이면 어떻게 될까?
         *
         * Service는 PostNotFoundException을 던진다. 그런데 이 예외를 HTTP 응답으로
         * 번역해주는 장치가 아직 없다 — 예외는 응답이 되지 못하고 그대로 터져나온다.
         * (실서버라면 사용자는 끔찍한 500 스택트레이스를 보게 된다)
         *
         * "없는 글이니 404를 주세요"는 누구의 책임인가? → Step 5의 주제!
         */
        @Test
        @DisplayName("없는 글이면... 예외가 응답으로 변환되지 못하고 그대로 터진다 (Step 5 예고)")
        void getPost_없는글_아직은예외가그대로터진다() throws Exception {
            // given
            given(boardService.getPost(99L)).willThrow(new PostNotFoundException(99L));

            // when & then : 404가 아니라 테스트 코드까지 예외가 올라와버린다!
            assertThatThrownBy(() -> mockMvc.perform(get("/api/posts/99")))
                    .hasCauseInstanceOf(PostNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("작성 POST /api/posts — 보안 경계 맛보기")
    class CreatePost {

        /**
         * SecurityConfig상 POST /api/posts는 인증 필요.
         * 로그인 없이 호출하면 Controller에 도달하기도 전에 Security가 401로 차단한다.
         * (인증/인가 테스트의 본편은 Step 6 — 여기서는 경계의 존재만 확인)
         */
        @Test
        @DisplayName("인증 없이 호출하면 401 (Controller에 도달하지도 못한다)")
        void createPost_미인증_401() throws Exception {
            // given
            String body = objectMapper.writeValueAsString(new PostCreateRequest("제목", "내용"));

            // when & then : csrf()를 붙인 이유 — 안 붙이면 CSRF 검증(403)이 먼저 걸려서
            //               "인증이 없어 401"이라는 이 테스트의 의도가 가려진다 (Step 6에서 상세히)
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
