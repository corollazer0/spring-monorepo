package com.testonboarding.advanced.step10.example;

import com.testonboarding.auth.jwt.JwtAuthenticationFilter;
import com.testonboarding.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [심화 Step 10 — example B] JWT 인증 필터의 단위 테스트
 *
 * Step 7에서 배운 서블릿 Mock 3총사를 그대로 적용한다.
 * 협력자인 JwtTokenProvider는 Mock이 아닌 "진짜"를 쓴다 —
 * 순수하고 빠르고 결정적이므로 (Step 2의 원칙: Mock은 I/O 경계에만).
 *
 * 검증의 초점: "토큰 상태별로 SecurityContext에 인증이 심기는가/안 심기는가",
 * 그리고 어느 경우든 "체인은 계속 진행되는가" (이 필터는 차단하지 않는다).
 */
@DisplayName("JWT 인증 필터 (단위 테스트)")
class JwtAuthenticationFilterTest {

    private static final String SECRET = "testcraft-jwt-secret-key-for-learning-only-do-not-use";

    private JwtTokenProvider provider;
    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, 3_600_000L);
        filter = new JwtAuthenticationFilter(provider);
        request = new MockHttpServletRequest("GET", "/api/v2/me");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext(); // ThreadLocal 정리 (Step 7의 교훈)
    }

    @Test
    @DisplayName("유효한 Bearer 토큰이면 SecurityContext에 인증이 심긴다")
    void doFilter_유효토큰_인증세팅() throws Exception {
        // given
        String token = provider.createToken("writer1", "USER");
        request.addHeader("Authorization", "Bearer " + token);

        // when
        filter.doFilter(request, response, chain);

        // then : 인증 주체와 권한("ROLE_" 접두사!)까지 검증
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("writer1");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        assertThat(chain.getRequest()).isSameAs(request); // 체인 진행
    }

    @Test
    @DisplayName("토큰이 없으면 인증을 심지 않지만 체인은 진행된다 (차단은 인가 단계의 일)")
    void doFilter_토큰없음_인증없이체인진행() throws Exception {
        // when : Authorization 헤더 없이
        filter.doFilter(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request); // 그래도 체인은 진행!
    }

    @Test
    @DisplayName("위조 토큰이면 인증을 심지 않는다")
    void doFilter_위조토큰_인증안심김() throws Exception {
        // given : 다른 키로 서명된 토큰
        JwtTokenProvider attacker = new JwtTokenProvider(
                "attacker-secret-key-that-is-long-enough-32bytes!", 3_600_000L);
        request.addHeader("Authorization", "Bearer " + attacker.createToken("admin", "ADMIN"));

        // when
        filter.doFilter(request, response, chain);

        // then : 공격자가 ADMIN을 자칭해도 서명 검증에서 거부된다
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Bearer 접두사가 없는 Authorization 헤더는 무시된다")
    void doFilter_Bearer접두사없음_무시() throws Exception {
        // given : Basic 인증 등 다른 방식의 헤더
        request.addHeader("Authorization", "Basic d3JpdGVyMTpwYXNz");

        // when
        filter.doFilter(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
