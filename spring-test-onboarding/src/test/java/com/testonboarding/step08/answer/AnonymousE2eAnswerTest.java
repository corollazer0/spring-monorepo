package com.testonboarding.step08.answer;

import com.testonboarding.board.dto.PostCreateRequest;
import com.testonboarding.support.RestSessionHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Step 8 — answer] AnonymousE2eExerciseTest 모범답안
 *
 * 채점 포인트:
 * - permitAll(읽기)과 인증 필요(쓰기)의 차이를 진짜 HTTP로 검증했는가
 * - 데이터를 만들지 않는 테스트라 정리(@Sql)가 필요 없다는 것을 이해했는가
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("비로그인 사용자 E2E (모범답안)")
class AnonymousE2eAnswerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("비로그인으로도 글 목록은 볼 수 있다")
    void 글목록조회_비로그인_200() {
        // given (TODO 1 답)
        RestSessionHelper session = new RestSessionHelper(restTemplate);

        // when (TODO 2 답)
        ResponseEntity<String> response = session.get("/api/posts", String.class);

        // then (TODO 3 답)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Spring 공부 기록");
    }

    @Test
    @DisplayName("비로그인으로 글을 쓰려 하면 401")
    void 글작성_비로그인_401() {
        // given (TODO 4 답) : CSRF 토큰은 받되 로그인은 하지 않는다 —
        //                     그래야 403(CSRF)이 아닌 401(인증)을 순수하게 검증할 수 있다
        RestSessionHelper session = new RestSessionHelper(restTemplate);
        session.fetchCsrfToken();

        // when (TODO 5 답)
        ResponseEntity<Void> response = session.post("/api/posts",
                new PostCreateRequest("몰래 쓰는 글", "안 되겠지?"), Void.class);

        // then (TODO 6 답)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
