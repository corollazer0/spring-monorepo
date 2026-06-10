package com.testonboarding.advanced.step10.example;

import com.testonboarding.auth.jwt.dto.TokenRequest;
import com.testonboarding.auth.jwt.dto.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [심화 Step 10 — example C] JWT 흐름 E2E: 토큰 발급 → Bearer로 API 호출
 *
 * 세션 E2E(Step 8)와 비교해보라:
 * - RestSessionHelper(쿠키 관리)가 필요 없다! — 상태가 토큰 안에 다 들어있으니까
 * - CSRF도 없다 — 세션 쿠키가 없으면 CSRF 공격 표면도 없다
 * - 헤더 한 줄(Authorization: Bearer ...)이 인증의 전부
 *
 * stateless 인증의 단순함이 테스트 코드의 단순함으로 그대로 드러난다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("JWT 인증 E2E")
class JwtAuthE2eTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("토큰 발급 → Bearer 헤더로 보호된 API 호출 성공")
    void 토큰발급후_보호된API호출_성공() {
        // ── 1) 시드 회원으로 토큰 발급 ──────────────────────────────
        ResponseEntity<TokenResponse> tokenResponse = restTemplate.postForEntity(
                "/api/v2/auth/token",
                new TokenRequest("writer1", "spring123!"),
                TokenResponse.class);

        assertThat(tokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String accessToken = tokenResponse.getBody().getAccessToken();
        assertThat(accessToken).isNotBlank();

        // ── 2) Bearer 헤더 한 줄로 보호된 API 호출 ──────────────────
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken); // "Authorization: Bearer {token}"

        ResponseEntity<String> meResponse = restTemplate.exchange(
                "/api/v2/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody())
                .contains("writer1")
                .contains("ROLE_USER");
    }

    @Test
    @DisplayName("틀린 비밀번호로는 토큰이 발급되지 않는다 (401)")
    void 토큰발급_틀린비밀번호_401() {
        // when
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                "/api/v2/auth/token",
                new TokenRequest("writer1", "wrong-password!"),
                TokenResponse.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
