package com.batchflow.step11.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.errorhandling.SkipDemoJobConfig;
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
 * [Batch Step 11 — exercise] Skip의 흔적을 장부에서 추적해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 SkipDemoJobTest를 참고)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 취지: 운영 장애 분석은 결국 장부(BATCH_STEP_EXECUTION) 읽기다.
 * "이 배치 어젯밤 몇 건 건너뛰었어요?"에 SQL 한 방으로 답해보자.
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step11.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {SkipDemoJobConfig.class, TestBatchConfig.class})
@DisplayName("Skip 장부 추적 (연습문제)")
class SkipLedgerExerciseTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    @DisplayName("장부에서 카운트 등식을 완성하라: READ = WRITE + PROCESS_SKIP")
    void skipDemoJob_장부_카운트등식() throws Exception {
        // when : Job을 실행하세요
        // TODO 1

        // then : BATCH_STEP_EXECUTION에서 READ_COUNT, WRITE_COUNT,
        //        PROCESS_SKIP_COUNT, ROLLBACK_COUNT를 SQL로 조회하고
        //        10 = 8 + 2, ROLLBACK=2 를 검증하세요
        // TODO 2 (힌트: queryForMap)
    }

    @Test
    @DisplayName("EXIT_CODE는 COMPLETED지만 skip이 있었다 — 운영 모니터링의 사각지대")
    void skipDemoJob_성공이지만skip존재_구분() throws Exception {
        // when : Job을 실행하세요
        // TODO 3

        // then(1) : 장부에서 Step의 EXIT_CODE가 'COMPLETED'인지 확인하세요
        // TODO 4
        // then(2) : 그런데 PROCESS_SKIP_COUNT는 0이 아니다!
        //           "성공했지만 일부를 버린 실행"을 어떻게 모니터링에서 구분할지
        //           자신의 아이디어를 한 줄 주석으로 적으세요
        // TODO 5
    }
}
