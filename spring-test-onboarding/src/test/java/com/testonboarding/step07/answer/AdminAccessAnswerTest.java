package com.testonboarding.step07.answer;

import com.testonboarding.board.controller.AdminController;
import com.testonboarding.board.service.BoardService;
import com.testonboarding.common.interceptor.AdminCheckInterceptor;
import com.testonboarding.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Step 7 — answer] AdminAccessExerciseTest 모범답안
 *
 * 채점 포인트:
 * - 통합(MockMvc)과 단위(직접 호출)의 역할 차이를 이해했는가
 * - X-Request-Id 헤더 검증으로 필터의 슬라이스 자동 포함을 확인했는가
 * - SecurityContextHolder 정리를 잊지 않았는가
 */
@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@DisplayName("관리자 접근 (모범답안)")
class AdminAccessAnswerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @Nested
    @DisplayName("통합: Security + Interceptor + Filter가 함께 동작")
    class MockMvcIntegration {

        @Test
        @WithMockUser(roles = "ADMIN") // (TODO 1 답)
        @DisplayName("ADMIN으로 stats 조회 → 200 + postCount + X-Request-Id 헤더")
        void stats_관리자_200과추적헤더() throws Exception {
            // given (TODO 2 답)
            given(boardService.countPosts()).willReturn(15L);

            // when & then (TODO 3~5 답)
            mockMvc.perform(get("/api/admin/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.postCount").value(15))
                    // 필터가 @WebMvcTest 슬라이스에 자동 포함되어 동작한 증거
                    .andExpect(header().exists("X-Request-Id"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("일반 USER로 stats 조회 → 403")
        void stats_일반사용자_403() throws Exception {
            // (TODO 6 답) Security(hasRole)가 인터셉터보다 앞단에서 먼저 차단한다 —
            // 인터셉터는 "Security가 풀렸을 때"를 대비한 2차 방어선
            mockMvc.perform(get("/api/admin/stats"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("단위: 인터셉터만 떼어내 검증")
    class InterceptorUnit {

        @Test
        @DisplayName("ADMIN이면 preHandle이 true")
        void preHandle_관리자_true() throws Exception {
            try {
                // given (TODO 7 답)
                AdminCheckInterceptor interceptor = new AdminCheckInterceptor();
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "admin", "password",
                                AuthorityUtils.createAuthorityList("ROLE_ADMIN")));

                // when & then (TODO 8 답)
                boolean result = interceptor.preHandle(
                        new MockHttpServletRequest("GET", "/api/admin/stats"),
                        new MockHttpServletResponse(),
                        new Object());
                assertThat(result).isTrue();
            } finally {
                // (TODO 9 답) ThreadLocal 저장소 — 안 지우면 다음 테스트로 인증이 샌다
                SecurityContextHolder.clearContext();
            }
        }
    }
}
