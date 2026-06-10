package com.testonboarding.step09.answer;

import com.testonboarding.comment.dto.CommentCreateRequest;
import com.testonboarding.support.RestSessionHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Step 9 — answer] 댓글 E2E (RANDOM_PORT)
 *
 * 전략: "로그인 → 댓글 작성 → 목록에 보인다"라는 여정의 연결 1본만 E2E로.
 * 세부 검증(순서/검증/404...)은 이미 아래층(answer 3종)이 다 했다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("댓글 E2E (캡스톤 모범답안)")
class CommentE2eAnswerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @Sql(scripts = "/sql/cleanup-comment-e2e.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("로그인 → 댓글 작성 → 목록 조회에 내 댓글이 보인다")
    void 댓글여정_로그인작성조회_성공() {
        RestSessionHelper session = new RestSessionHelper(restTemplate);

        // 1) 시드 회원으로 진짜 로그인
        boolean loginSuccess = session.login("writer1", "spring123!");
        assertThat(loginSuccess).isTrue();

        // 2) 3번 글(시드 댓글 없음)에 댓글 작성
        ResponseEntity<Void> createResponse = session.post("/api/posts/3/comments",
                new CommentCreateRequest("E2E 댓글입니다"), Void.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 3) 목록 조회 — 방금 단 댓글이 실제로 보인다 (여정의 연결!)
        ResponseEntity<String> listResponse = session.get("/api/posts/3/comments", String.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody())
                .contains("E2E 댓글입니다")
                .contains("writer1");
    }
}
