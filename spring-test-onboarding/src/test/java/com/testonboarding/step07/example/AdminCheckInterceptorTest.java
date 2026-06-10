package com.testonboarding.step07.example;

import com.testonboarding.common.interceptor.AdminCheckInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Step 7 — example B] HandlerInterceptor의 단위 테스트
 *
 * 인터셉터도 필터처럼 "그냥 클래스"다 — preHandle을 직접 호출해 분기를 검증한다.
 *
 * 이 인터셉터는 SecurityContextHolder에서 현재 사용자를 읽으므로,
 * given 단계에서 SecurityContext를 직접 구성한다.
 * (@WithMockUser는 Spring 테스트 컨텍스트용 — 순수 단위 테스트에서는 이렇게 수동 세팅)
 *
 * ⚠️ SecurityContextHolder는 static(ThreadLocal) 저장소 — 테스트 후 반드시 clearContext()!
 *    안 지우면 다음 테스트에 인증 상태가 새어 들어가 "혼자 돌리면 통과, 같이 돌리면 실패"라는
 *    최악의 비결정성 버그가 생긴다.
 */
@DisplayName("관리자 인터셉터 (단위 테스트)")
class AdminCheckInterceptorTest {

    private AdminCheckInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new AdminCheckInterceptor();
        request = new MockHttpServletRequest("GET", "/api/admin/stats");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext(); // 테스트 격리 필수!
    }

    @Test
    @DisplayName("ADMIN 권한이면 true를 반환해 Controller로 진행시킨다")
    void preHandle_관리자_true반환() throws Exception {
        // given
        authenticateAs("admin", "ROLE_ADMIN");

        // when
        boolean result = interceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(200); // 응답에 손대지 않았다
    }

    @Test
    @DisplayName("일반 USER면 false + 403 + 에러 JSON으로 직접 응답한다")
    void preHandle_일반사용자_false와403() throws Exception {
        // given
        authenticateAs("writer1", "ROLE_USER");

        // when
        boolean result = interceptor.preHandle(request, response, new Object());

        // then : 차단(false) + 인터셉터가 직접 써넣은 응답 검증
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("관리자만 접근할 수 있습니다");
    }

    @Test
    @DisplayName("인증 정보가 아예 없어도 false + 403 (null 안전성)")
    void preHandle_미인증_false와403() throws Exception {
        // given : SecurityContext가 비어있는 상태 (clearContext 직후와 동일)

        // when
        boolean result = interceptor.preHandle(request, response, new Object());

        // then : NullPointerException이 아니라 정중한 403 — 방어 코드 검증
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    private void authenticateAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username, "password", AuthorityUtils.createAuthorityList(role)));
    }
}
