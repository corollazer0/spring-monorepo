package com.testonboarding.advanced.step10.answer;

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
 * [심화 Step 10 — answer] JwtAccessExerciseTest 모범답안
 *
 * 채점 포인트:
 * - "토큰 없음"과 "위조 토큰" 모두 같은 401로 끝나지만, 내부 경로를 설명할 수 있는가
 *   (없음 → 필터 통과(인증 없음) → 인가 차단 / 위조 → 서명 검증 실패 → 인증 없음 → 인가 차단)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("JWT 접근 차단 (모범답안)")
class JwtAccessAnswerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("토큰 없이 보호된 API 호출 → 401")
    void 보호된API_토큰없음_401() {
        // when (TODO 1 답)
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v2/me", String.class);

        // then (TODO 2 답)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("위조 토큰으로 보호된 API 호출 → 401")
    void 보호된API_위조토큰_401() {
        // given (TODO 3 답)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("this.is.fake");

        // when (TODO 4 답)
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v2/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        // then (TODO 5 답)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
