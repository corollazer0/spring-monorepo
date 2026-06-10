package com.testonboarding.step07.exercise;

import com.testonboarding.board.controller.AdminController;
import com.testonboarding.board.service.BoardService;
import com.testonboarding.config.SecurityConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * [Step 7 — exercise] 관리자 접근을 단위 + 통합 두 방식으로 검증해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example A/B를 참고)
 * 3. .\gradlew :spring-test-onboarding:test 로 통과를 확인한다
 *
 * 알아둘 것: @WebMvcTest는 @Component인 Filter / HandlerInterceptor / WebMvcConfigurer를
 * 자동으로 스캔해 포함한다 — 그래서 이 통합 테스트에는 RequestLoggingFilter와
 * AdminCheckInterceptor가 이미 끼어 있다!
 */
@Disabled("과제: docs/test/education/FOR-Test-Step07.md 참고 후 @Disabled를 제거하고 완성하세요")
@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@DisplayName("관리자 접근 (연습문제)")
class AdminAccessExerciseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @Nested
    @DisplayName("통합: Security + Interceptor + Filter가 함께 동작")
    class MockMvcIntegration {

        @Test
        @DisplayName("ADMIN으로 stats 조회 → 200 + postCount + X-Request-Id 헤더")
        void stats_관리자_200과추적헤더() throws Exception {
            // given :
            // TODO 1: 이 테스트를 ADMIN 권한으로 만드세요 (@WithMockUser(roles = "ADMIN"))
            // TODO 2: boardService.countPosts()가 15L을 돌려주도록 stubbing 하세요

            // when & then :
            // TODO 3: GET /api/admin/stats 를 호출하세요
            // TODO 4: 상태 200 + jsonPath $.postCount 가 15인지 검증하세요
            // TODO 5: 응답에 X-Request-Id 헤더가 존재하는지 검증하세요
            //         (힌트: header().exists("X-Request-Id") — 필터가 슬라이스에 자동 포함된 증거!)
        }

        @Test
        @DisplayName("일반 USER로 stats 조회 → 403")
        void stats_일반사용자_403() throws Exception {
            // TODO 6: USER 권한으로 요청하고 403을 검증하세요
            //         (Security의 hasRole과 인터셉터, 누가 먼저 막았을까? 문서 6장 참고)
        }
    }

    @Nested
    @DisplayName("단위: 인터셉터만 떼어내 검증")
    class InterceptorUnit {

        @Test
        @DisplayName("ADMIN이면 preHandle이 true")
        void preHandle_관리자_true() throws Exception {
            // given :
            // TODO 7: AdminCheckInterceptor를 new로 만들고,
            //         SecurityContextHolder에 ROLE_ADMIN 인증을 세팅하세요
            //         (example B의 authenticateAs 헬퍼를 참고)

            // when & then :
            // TODO 8: preHandle을 직접 호출해 true를 검증하세요
            // TODO 9: SecurityContextHolder.clearContext()로 정리하세요 (왜 필수인지 설명할 수 있나요?)
        }
    }
}
