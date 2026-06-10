package com.testonboarding.advanced.step10.exercise;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

/**
 * [심화 Step 10 — exercise] 토큰 없는/위조된 접근을 직접 검증해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example C의 JwtAuthE2eTest를 참고)
 * 3. .\gradlew :spring-test-onboarding:test 로 통과를 확인한다
 *
 * 힌트:
 * - 헤더 없는 GET: restTemplate.getForEntity("/api/v2/me", String.class)
 * - 가짜 토큰 헤더: headers.setBearerAuth("this.is.fake")
 */
@Disabled("과제: docs/test/education/FOR-Test-Step10.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("JWT 접근 차단 (연습문제)")
class JwtAccessExerciseTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("토큰 없이 보호된 API 호출 → 401")
    void 보호된API_토큰없음_401() {
        // when : Authorization 헤더 없이 GET /api/v2/me 를 호출하세요
        // TODO 1

        // then : 401 UNAUTHORIZED를 검증하세요
        // TODO 2
    }

    @Test
    @DisplayName("위조 토큰으로 보호된 API 호출 → 401")
    void 보호된API_위조토큰_401() {
        // given : 가짜 토큰을 Bearer 헤더에 실으세요
        // TODO 3

        // when : GET /api/v2/me 를 호출하세요 (restTemplate.exchange 사용)
        // TODO 4

        // then : 401 UNAUTHORIZED를 검증하세요
        //        (필터가 인증을 안 심었고 → 인가 단계가 차단했다 — 두 단계의 합작!)
        // TODO 5
    }
}
