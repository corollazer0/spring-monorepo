package com.testonboarding.step12.exercise;

import com.testonboarding.board.service.BoardService;
import com.testonboarding.comment.service.CommentService;
import com.testonboarding.config.SecurityConfig;
import com.testonboarding.web.PostViewController;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * [Step 12 — exercise] 목록 화면의 페이징과 빈 목록을 직접 검증해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example A의 PostViewControllerTest를 참고)
 * 3. .\gradlew :spring-test-onboarding:test 로 통과를 확인한다
 *
 * 힌트:
 * - 쿼리 파라미터: get("/posts").param("page", "3")
 * - 빈 목록: willReturn(Collections.emptyList())
 * - 모델 값 검증: model().attribute("page", 3)
 */
@Disabled("과제: docs/test/education/FOR-Test-Step12.md 참고 후 @Disabled를 제거하고 완성하세요")
@WebMvcTest(PostViewController.class)
@Import(SecurityConfig.class)
@DisplayName("목록 화면 페이징 (연습문제)")
class PostListViewExerciseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @MockBean
    private CommentService commentService;

    @Test
    @DisplayName("page 파라미터가 서비스와 모델에 정확히 전달된다")
    void list_3페이지요청_서비스와모델에전달() throws Exception {
        // given :
        // TODO 1: boardService.getPosts(3, 10)이 빈 목록을 돌려주도록 stubbing 하세요

        // when & then :
        // TODO 2: GET /posts?page=3 을 호출하세요
        // TODO 3: 상태 200 + 뷰 이름 post/list 를 검증하세요
        // TODO 4: 모델의 page 속성이 3인지 검증하세요
        // TODO 5: verify로 getPosts(3, 10) 호출을 확인하세요 (바인딩 검증!)
    }

    @Test
    @DisplayName("게시글이 없으면 빈 안내 문구가 렌더링된다")
    void list_빈목록_안내문구렌더링() throws Exception {
        // given :
        // TODO 6: 빈 목록을 stubbing 하세요

        // when & then :
        // TODO 7: 렌더링된 HTML에 "게시글이 없습니다"가 포함되는지 검증하세요
        //         (템플릿의 th:if="${#lists.isEmpty(posts)}" 분기가 동작하는 증명!)
    }
}
