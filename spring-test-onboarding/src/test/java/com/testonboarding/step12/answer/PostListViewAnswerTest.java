package com.testonboarding.step12.answer;

import com.testonboarding.board.service.BoardService;
import com.testonboarding.comment.service.CommentService;
import com.testonboarding.config.SecurityConfig;
import com.testonboarding.web.PostViewController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * [Step 12 — answer] PostListViewExerciseTest 모범답안
 *
 * 채점 포인트:
 * - 파라미터 바인딩을 모델 검증 + verify "양쪽"으로 증명했는가
 * - 템플릿의 조건 분기(th:if)까지 렌더링 검증으로 확인했는가
 */
@WebMvcTest(PostViewController.class)
@Import(SecurityConfig.class)
@DisplayName("목록 화면 페이징 (모범답안)")
class PostListViewAnswerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @MockBean
    private CommentService commentService;

    @Test
    @DisplayName("page 파라미터가 서비스와 모델에 정확히 전달된다")
    void list_3페이지요청_서비스와모델에전달() throws Exception {
        // given (TODO 1 답)
        given(boardService.getPosts(3, 10)).willReturn(Collections.emptyList());

        // when & then (TODO 2~4 답)
        mockMvc.perform(get("/posts").param("page", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("post/list"))
                .andExpect(model().attribute("page", 3));

        // then (TODO 5 답) : "?page=3"이 int 3으로 변환되어 서비스까지 도달했다
        verify(boardService).getPosts(3, 10);
    }

    @Test
    @DisplayName("게시글이 없으면 빈 안내 문구가 렌더링된다")
    void list_빈목록_안내문구렌더링() throws Exception {
        // given (TODO 6 답)
        given(boardService.getPosts(1, 10)).willReturn(Collections.emptyList());

        // when & then (TODO 7 답) : 템플릿의 th:if 분기가 실제로 동작한다
        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("게시글이 없습니다")));
    }
}
