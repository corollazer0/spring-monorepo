package com.testonboarding.step08.exercise;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

/**
 * [Step 8 — exercise] 비로그인 사용자의 E2E를 직접 작성해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example A의 MemberJourneyE2eTest를 참고)
 * 3. .\gradlew :spring-test-onboarding:test 로 통과를 확인한다
 *
 * 힌트:
 * - RestSessionHelper를 만들어 fetchCsrfToken()만 하고 login은 하지 마세요
 * - 글 목록 조회는 permitAll, 글 작성은 인증 필요 — 이 차이가 진짜 HTTP에서도 지켜지는지!
 */
@Disabled("과제: docs/test/education/FOR-Test-Step08.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("비로그인 사용자 E2E (연습문제)")
class AnonymousE2eExerciseTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("비로그인으로도 글 목록은 볼 수 있다")
    void 글목록조회_비로그인_200() {
        // given : 세션 헬퍼를 만드세요 (로그인은 하지 않습니다)
        // TODO 1

        // when : GET /api/posts 를 호출하세요 (String.class로 받으면 됩니다)
        // TODO 2

        // then : 상태가 200 OK이고, 본문에 시드 데이터의 제목 일부(예: Spring 공부 기록)가 있는지 검증하세요
        // TODO 3
    }

    @Test
    @DisplayName("비로그인으로 글을 쓰려 하면 401")
    void 글작성_비로그인_401() {
        // given : 세션 헬퍼 + CSRF 토큰만 받으세요 (로그인 없이!)
        // TODO 4

        // when : POST /api/posts 로 글 작성을 시도하세요
        // TODO 5

        // then : 상태가 401 UNAUTHORIZED인지 검증하세요
        //        (MockMvc가 아닌 진짜 HTTP에서도 보안 경계가 지켜진다는 최종 증명!)
        // TODO 6
    }
}
