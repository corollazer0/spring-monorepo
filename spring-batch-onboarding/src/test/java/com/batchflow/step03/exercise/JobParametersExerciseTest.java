package com.batchflow.step03.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.parameters.DailyGreetingJobConfig;
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
 * [Batch Step 3 — exercise] 파라미터의 흔적을 장부에서 추적해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 DailyGreetingJobTest를 참고)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 힌트:
 * - 파라미터는 BATCH_JOB_EXECUTION_PARAMS 테이블에 KEY_NAME/STRING_VAL로 기록된다
 * - jobLauncherTestUtils.getUniqueJobParameters() = Step 2에서 launchJob()이 몰래 쓰던 바로 그것
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step03.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {DailyGreetingJobConfig.class, TestBatchConfig.class})
@DisplayName("JobParameters 추적 (연습문제)")
class JobParametersExerciseTest {

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
    @DisplayName("실행 파라미터가 장부(BATCH_JOB_EXECUTION_PARAMS)에 기록된다")
    void launchJob_파라미터_장부기록확인() throws Exception {
        // given : targetDate=2026-06-11 파라미터를 만드세요 (JobParametersBuilder)
        // TODO 1

        // when : Job을 실행하세요
        // TODO 2

        // then : BATCH_JOB_EXECUTION_PARAMS에서 KEY_NAME='targetDate'인 행의
        //        STRING_VAL이 '2026-06-11'인지 JdbcTemplate로 검증하세요
        // TODO 3
    }

    @Test
    @DisplayName("getUniqueJobParameters로는 같은 Job을 몇 번이든 실행할 수 있다")
    void launchJob_unique파라미터_반복실행성공() throws Exception {
        // when : getUniqueJobParameters()로 만든 파라미터로 두 번 실행하세요
        // TODO 4

        // then : 둘 다 COMPLETED인지 검증하고,
        //        왜 거부되지 않는지 한 줄 주석으로 설명하세요 (JobInstance 관점에서!)
        // TODO 5
    }
}
