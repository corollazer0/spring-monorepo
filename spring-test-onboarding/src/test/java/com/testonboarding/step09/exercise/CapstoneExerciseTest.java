package com.testonboarding.step09.exercise;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Step 9 — 캡스톤] 댓글 기능의 테스트를 "처음부터 끝까지 스스로" 설계하고 작성하세요
 *
 * 지금까지와 다른 점: 이번에는 TODO 골격이 없습니다.
 * 요구사항 문서를 읽고, 어떤 것을 단위/슬라이스/E2E로 검증할지 스스로 결정하세요.
 *
 * ── 진행 방법 ──────────────────────────────────────────────
 * 1. docs/test/education/FOR-Test-Step09-Requirements.md 를 정독한다
 *    (요구사항 + 평가 체크리스트가 들어있다)
 * 2. 프로덕션 코드를 읽는다: comment 패키지 (domain/dao/service/controller/dto)
 * 3. 이 패키지(step09.exercise) 아래에 자신만의 테스트 클래스를 만든다. 권장 구성:
 *      CommentServiceTest      — Mockito 단위        (Step 2 참고)
 *      CommentDaoTest          — @MybatisTest        (Step 3 참고)
 *      CommentControllerTest   — @WebMvcTest         (Step 4~6 참고)
 *      CommentE2eTest          — RANDOM_PORT E2E     (Step 8 참고)
 * 4. 체크리스트로 자가 채점한다
 * 5. 막혔다면(30분 룰!) step09/answer 의 모범답안과 비교한다
 *
 * ── 시드 데이터 (data.sql) ─────────────────────────────────
 * - 1번 글에 댓글 2건(등록순: writer2 → writer1), 2번 글에 1건
 *
 * 아래의 워밍업 테스트는 시작을 돕는 준비운동입니다 — @Disabled를 지우고 시작하세요.
 */
@Disabled("캡스톤: docs/test/education/FOR-Test-Step09-Requirements.md 를 읽고 시작하세요")
@DisplayName("캡스톤 워밍업")
class CapstoneExerciseTest {

    @Test
    @DisplayName("워밍업: 요구사항을 읽었다면 이 질문에 답할 수 있다")
    void 워밍업_테스트전략질문() {
        // 다음 각 검증을 어느 계층의 테스트로 작성할지 결정하고, 이유를 주석으로 적어보세요.
        // (정답은 하나가 아닙니다 — 중요한 건 "이유"입니다)
        //
        // (1) "없는 게시글에 댓글 작성 → 404"      : 어느 계층? 왜?
        // (2) "댓글이 등록순으로 조회된다"           : 어느 계층? 왜?
        // (3) "내용 빈 값 → 400 + fieldErrors"     : 어느 계층? 왜?
        // (4) "비로그인 댓글 작성 → 401"            : 어느 계층? 왜?
        // (5) "로그인→댓글작성→목록에 보인다"        : 어느 계층? 왜?

        assertThat(true).isTrue(); // 답을 적었다면 통과시키고 본 테스트 작성으로!
    }
}
