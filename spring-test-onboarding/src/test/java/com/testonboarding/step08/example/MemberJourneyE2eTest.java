package com.testonboarding.step08.example;

import com.testonboarding.member.dto.MemberSignupRequest;
import com.testonboarding.board.dto.PostCreateRequest;
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
 * [Step 8 — example] 진짜 E2E: 회원가입 → 로그인 → 글 작성 → 조회
 *
 * 문제 상황: 조각(Step 1~7)은 전부 통과했다. 하지만 "전체가 한 몸으로 도는가"는
 * 아무도 증명하지 않았다. @WithMockUser는 진짜 로그인을 검증한 적이 없고,
 * @MockBean으로 채웠던 자리들이 실제 빈으로 연결될 때의 문제도 모른다.
 *
 * 해결: @SpringBootTest(webEnvironment = RANDOM_PORT)
 * - 모든 빈 + 진짜 Tomcat을 임의 포트에 띄운다
 * - TestRestTemplate으로 "진짜 HTTP"를 보낸다 — Mock이 단 하나도 없다
 * - 세션 쿠키, CSRF 쿠키도 브라우저처럼 직접 주고받는다 (RestSessionHelper)
 *
 * ⚠️ 이 방식의 대가:
 * 1. 느리다 (컨텍스트 + 서버 기동) → 핵심 시나리오만 소수 정예로
 * 2. @Transactional 롤백이 동작하지 않는다! — 테스트는 테스트 스레드에서,
 *    요청 처리는 서버 스레드에서 돌기 때문에 트랜잭션이 분리된다.
 *    → @Sql(AFTER_TEST_METHOD)로 만든 데이터를 직접 치운다 (아래 참고)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("회원 여정 E2E (RANDOM_PORT + TestRestTemplate)")
class MemberJourneyE2eTest {

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * 회원가입부터 글 조회까지 — 실제 사용자의 여정 전체를 한 시나리오로.
     *
     * E2E는 "스텝별 단언"이 아니라 "여정의 연결"을 검증한다:
     * 가입한 계정으로 로그인이 되고, 그 세션으로 글이 써지고, 그 글이 조회된다.
     */
    @Test
    @Sql(scripts = "/sql/cleanup-journey.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("회원가입 → 로그인 → 글 작성 → 조회 전체 여정")
    void 전체여정_회원가입부터글조회까지_성공() {
        RestSessionHelper session = new RestSessionHelper(restTemplate);

        // ── 1) 회원가입 (비로그인 + CSRF 토큰 필요) ──────────────────────
        session.fetchCsrfToken();
        ResponseEntity<Void> signupResponse = session.post("/api/members",
                new MemberSignupRequest("journey1", "spring123!", "여정이"), Void.class);
        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // ── 2) 진짜 폼 로그인 — UserDetailsService + PasswordEncoder가 실제로 동작한다.
        //       @WithMockUser가 영영 검증하지 못하는 "로그인 절차 그 자체"의 검증!
        boolean loginSuccess = session.login("journey1", "spring123!");
        assertThat(loginSuccess).as("가입한 계정으로 로그인이 되어야 한다").isTrue();

        // ── 3) 세션 확인: 내 정보 조회 ──────────────────────────────────
        ResponseEntity<String> meResponse = session.get("/api/members/me", String.class);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).contains("journey1").contains("여정이");

        // ── 4) 글 작성 (세션 + CSRF 동봉) ───────────────────────────────
        ResponseEntity<Void> createResponse = session.post("/api/posts",
                new PostCreateRequest("E2E로 쓴 첫 글", "전 레이어가 한 몸으로 돌았다"), Void.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String location = createResponse.getHeaders().getFirst("Location");
        assertThat(location).startsWith("/api/posts/");

        // ── 5) 작성한 글 조회 — Location이 가리키는 실제 리소스 확인 ──────
        ResponseEntity<String> postResponse = session.get(location, String.class);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(postResponse.getBody())
                .contains("E2E로 쓴 첫 글")
                .contains("journey1"); // writer가 로그인 사용자로 기록됐다
    }

    @Test
    @DisplayName("틀린 비밀번호로 로그인하면 실패한다 (진짜 비밀번호 대조 검증)")
    void 로그인_틀린비밀번호_실패() {
        RestSessionHelper session = new RestSessionHelper(restTemplate);

        // when : 시드 회원 writer1의 비밀번호를 틀리게 입력
        boolean loginSuccess = session.login("writer1", "wrong-password!");

        // then : DelegatingPasswordEncoder의 대조가 실제로 동작한다는 증거.
        //        @WithMockUser 세계에서는 절대 잡을 수 없는 검증이다.
        assertThat(loginSuccess).isFalse();
    }
}
