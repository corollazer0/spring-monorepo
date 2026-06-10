package com.testonboarding.step06.answer;

import com.testonboarding.board.controller.BoardController;
import com.testonboarding.board.service.BoardService;
import com.testonboarding.common.exception.NotPostOwnerException;
import com.testonboarding.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Step 6 — answer] BoardDeleteSecurityExerciseTest 모범답안
 *
 * 채점 포인트:
 * - 401(인증 없음)과 403(소유자 아님)을 정확히 구분했는가
 * - void 메서드 stubbing에 willThrow를 썼는가
 * - 성공 케이스에서 "로그인 사용자명으로 위임"을 verify로 증명했는가
 */
@WebMvcTest(BoardController.class)
@Import(SecurityConfig.class)
@DisplayName("게시글 삭제 보안 (모범답안)")
class BoardDeleteSecurityAnswerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @Test
    @DisplayName("미인증 삭제 시도 → 401")
    void deletePost_미인증_401() throws Exception {
        // when & then (TODO 1 답)
        mockMvc.perform(delete("/api/posts/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "hacker") // (TODO 2 답)
    @DisplayName("타인 글 삭제 시도 → 403")
    void deletePost_타인글_403() throws Exception {
        // given (TODO 3 답) : void 메서드는 given(...).willThrow가 아니라 willThrow(...).given 순서!
        willThrow(new NotPostOwnerException(1L, "hacker"))
                .given(boardService).deletePost(1L, "hacker");

        // when & then (TODO 4 답)
        mockMvc.perform(delete("/api/posts/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "writer1") // (TODO 5 답)
    @DisplayName("본인 글 삭제 → 204")
    void deletePost_본인글_204() throws Exception {
        // when & then (TODO 6 답)
        mockMvc.perform(delete("/api/posts/1").with(csrf()))
                .andExpect(status().isNoContent());

        // then (TODO 7 답) : 다른 사용자 이름으로 위임되는 버그를 잡는 검증
        verify(boardService).deletePost(1L, "writer1");
    }
}
