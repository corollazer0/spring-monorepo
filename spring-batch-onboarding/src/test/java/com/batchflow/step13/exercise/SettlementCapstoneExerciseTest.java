package com.batchflow.step13.exercise;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 13 — 캡스톤] 일일 정산 Job의 테스트를 "처음부터 끝까지 스스로" 설계하세요
 *
 * 지금까지와 다른 점: TODO 골격이 없습니다.
 * 요구사항 문서를 읽고, 어떤 것을 단위/단독/통합으로 검증할지 스스로 결정하세요.
 *
 * ── 진행 방법 ──────────────────────────────────────────────
 * 1. docs/batch/education/FOR-BatchFlow-Step13-Requirements.md 정독
 *    (요구사항 + 평가 체크리스트)
 * 2. 프로덕션 코드 읽기: job/settlement/DailySettlementJobConfig,
 *    processor/SettlementProcessor, domain/{DailyTxSummary, Settlement}
 * 3. 이 패키지(step13.exercise) 아래에 자신만의 테스트 클래스를 만든다. 권장:
 *      SettlementProcessorTest  — 순수 단위 (Step 9 참고)
 *      SettlementReaderTest     — 리더 단독 (Step 8의 StepScopeTestUtils 참고)
 *      SettlementJobTest        — Job 통합 + 재실행 멱등성 (Step 10, 12 참고)
 * 4. 체크리스트로 자가 채점
 * 5. 막혔다면(30분 룰!) step13/answer 의 모범답안과 비교
 *
 * ── 시드 기준값 (data.sql, Step 6에서 봉인됨) ──────────────
 * 2026-06-10자 거래 9건 / 정산 대상 5명:
 *   m1: 입금 30000, 출금 5000  → 순액 +25000 (3건)
 *   m2: 입금 50000            → 순액 +50000 (1건)
 *   m3: 출금 10000            → 순액 -10000 (2건)  ← 음수!
 *   m4: 입금 = 출금 15000     → 순액 0      (2건)  ← 경계!
 *   m5: 입금 8000             → 순액 +8000  (1건)
 */
@Disabled("캡스톤: docs/batch/education/FOR-BatchFlow-Step13-Requirements.md 를 읽고 시작하세요")
@DisplayName("정산 캡스톤 워밍업")
class SettlementCapstoneExerciseTest {

    @Test
    @DisplayName("워밍업: 요구사항을 읽었다면 이 질문에 답할 수 있다")
    void 워밍업_테스트전략질문() {
        // 다음 각 검증을 어느 방식으로 할지 결정하고, 이유를 주석으로 적어보세요.
        //
        // (1) "순액 = 입금 - 출금, 음수 가능"        : 단위/단독/통합 중? 왜?
        // (2) "GROUP BY 집계가 회원별로 정확"        : 어느 방식? 왜?
        // (3) "재실행해도 정산이 중복되지 않는다"     : 어느 방식? 왜? (clearStep의 역할은?)
        // (4) "거래가 없는 날은 0건으로 정상 종료"    : 어느 방식? 왜?
        // (5) "settlement에 실제 INSERT된 값 검증"   : 어느 방식? 왜?

        assertThat(true).isTrue(); // 답을 적었다면 통과시키고 본 테스트 작성으로!
    }
}
