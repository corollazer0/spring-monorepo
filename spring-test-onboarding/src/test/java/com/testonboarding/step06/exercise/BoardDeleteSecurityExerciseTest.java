package com.testonboarding.step06.exercise;

import com.testonboarding.board.controller.BoardController;
import com.testonboarding.board.service.BoardService;
import com.testonboarding.config.SecurityConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * [Step 6 — exercise] 게시글 삭제 보안 시나리오 3종을 직접 작성해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 BoardSecurityTest를 참고)
 * 3. .\gradlew :spring-test-onboarding:test 로 통과를 확인한다
 *
 * 검증 대상: DELETE /api/posts/{postId}
 * - 컨트롤러는 성공 시 204(No Content)를 반환한다
 * - Service의 deletePost(postId, username)는 작성자가 아니면 NotPostOwnerException을 던진다
 *
 * 힌트:
 * - DELETE 요청: MockMvcRequestBuilders.delete("/api/posts/1")
 * - 반환값 없는(void) Mock 메서드에 예외를 심을 때: willThrow(...).given(mock).method(...)
 */
@Disabled("과제: docs/test/education/FOR-Test-Step06.md 참고 후 @Disabled를 제거하고 완성하세요")
@WebMvcTest(BoardController.class)
@Import(SecurityConfig.class)
@DisplayName("게시글 삭제 보안 (연습문제)")
class BoardDeleteSecurityExerciseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @Test
    @DisplayName("미인증 삭제 시도 → 401")
    void deletePost_미인증_401() throws Exception {
        // when & then :
        // TODO 1: 인증 없이 DELETE /api/posts/1 을 호출하고 401을 검증하세요
        //         (csrf()는 붙이세요 — 안 붙이면 403이 먼저 떠서 의도가 가려집니다)
    }

    @Test
    @DisplayName("타인 글 삭제 시도 → 403")
    void deletePost_타인글_403() throws Exception {
        // given :
        // TODO 2: 이 테스트를 hacker로 로그인한 상태로 만드세요 (@WithMockUser를 메서드에 붙여도 됩니다)
        // TODO 3: boardService.deletePost(1L, "hacker") 호출 시 NotPostOwnerException이
        //         발생하도록 stubbing 하세요 (void 메서드 → willThrow 사용)

        // when & then :
        // TODO 4: DELETE 요청을 보내고 403을 검증하세요
    }

    @Test
    @DisplayName("본인 글 삭제 → 204")
    void deletePost_본인글_204() throws Exception {
        // given :
        // TODO 5: writer1로 로그인한 상태를 만드세요

        // when & then :
        // TODO 6: DELETE 요청을 보내고 204(No Content)를 검증하세요
        // TODO 7: boardService.deletePost(1L, "writer1")이 호출됐는지 verify 하세요
        //         ("로그인한 그 사용자"의 이름으로 삭제가 위임됐다는 증명!)
    }
}
