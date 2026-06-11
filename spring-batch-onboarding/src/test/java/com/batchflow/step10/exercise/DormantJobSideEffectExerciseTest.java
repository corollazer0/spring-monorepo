package com.batchflow.step10.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.dormant.DormantMemberJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * [Batch Step 10 — exercise] "건드리지 말아야 할 것"을 검증해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 DormantMemberJobTest를 참고 — 원상복구 포함!)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 취지: 대량 UPDATE 배치의 가장 무서운 사고는 "대상이 아닌 행을 건드리는 것".
 * 전환"된" 것만큼 전환"되지 않은" 것의 검증이 중요하다.
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step10.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {DormantMemberJobConfig.class, TestBatchConfig.class})
@DisplayName("휴면 전환의 부수효과 경계 (연습문제)")
class DormantJobSideEffectExerciseTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        // TODO 1: example처럼 회원 21~30을 시드 상태(ACTIVE, dormant_at NULL)로 복구하세요
        //         (이게 없으면 "혼자는 통과, 같이 돌리면 실패"가 됩니다 — 왜인지 설명해보세요)
    }

    @Test
    @DisplayName("최근 로그인 회원(1~20)은 한 명도 건드리지 않는다")
    void dormantMemberJob_비대상_영향없음() throws Exception {
        // given & when : cutoffDate=2025-06-11, dormantAt=2026-06-11T03:00:00 로 실행하세요
        // TODO 2

        // then : member_id 1~20 중 status가 ACTIVE가 아니거나 dormant_at이 채워진
        //        행이 "0건"인지 SQL로 검증하세요
        // TODO 3
    }

    @Test
    @DisplayName("WITHDRAWN(탈퇴) 회원도 그대로다")
    void dormantMemberJob_탈퇴회원_영향없음() throws Exception {
        // given & when : Job을 실행하세요 (위와 다른 run 파라미터!)
        // TODO 4

        // then : WITHDRAWN 회원 수가 여전히 5명인지 검증하세요
        // TODO 5
    }
}
