package com.testonboarding.step05.example;

import com.testonboarding.board.controller.BoardController;
import com.testonboarding.board.service.BoardService;
import com.testonboarding.common.exception.PostNotFoundException;
import com.testonboarding.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Step 5 — example C] Step 4 cliffhanger의 해결
 *
 * Step 4에서 PostNotFoundException은 HTTP 응답으로 번역되지 못하고 그대로 터졌다
 * (step04/example/BoardControllerTest의 보존된 테스트 참고 — advice를 일부러 제외했다).
 *
 * 이제 GlobalExceptionHandler(@RestControllerAdvice)가 같은 예외를 받아
 * "404 + 규약된 에러 JSON"으로 번역한다. 같은 요청, 완전히 다른 결말.
 *
 * 참고: @WebMvcTest는 @RestControllerAdvice를 자동으로 슬라이스에 포함한다 —
 *       그래서 별도 @Import 없이도 핸들러가 동작한다.
 */
@WebMvcTest(BoardController.class)
@Import(SecurityConfig.class)
@DisplayName("게시판 예외 → HTTP 응답 번역 (GlobalExceptionHandler)")
class BoardExceptionHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @Test
    @DisplayName("없는 글 조회는 이제 404와 에러 응답 JSON으로 번역된다")
    void getPost_없는글_404와에러응답() throws Exception {
        // given : Step 4의 cliffhanger와 완전히 동일한 상황
        given(boardService.getPost(99L)).willThrow(new PostNotFoundException(99L));

        // when & then : 예외가 터지는 대신, 사용자는 정중한 404 JSON을 받는다
        mockMvc.perform(get("/api/posts/99"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다. postId=99"));
    }

    @Test
    @DisplayName("예상 못 한 예외는 500 + 내부 정보가 숨겨진 메시지로 번역된다")
    void getPost_예상못한예외_500과안전한메시지() throws Exception {
        // given : DB 커넥션 끊김 같은 시스템 예외 상황
        given(boardService.getPost(1L)).willThrow(new IllegalStateException("DB connection lost!"));

        // when & then : 핵심 — 응답에 "DB connection lost!"가 절대 노출되지 않는다.
        //               내부 사정은 로그에만, 사용자에겐 일반화된 메시지만.
        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다"));
    }
}
