package com.batchflow.step12.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.restart.RestartableDormantJobConfig;
import com.batchflow.processor.SabotageProcessor;
import org.junit.jupiter.api.AfterEach;
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
 * [Batch Step 12 — exercise] 재시작의 흔적을 장부에서 추적해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 RestartableDormantJobTest를 참고)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 취지: Step 3에서 외운 "JobInstance 1 : JobExecution N"을 SQL로 직접 목격한다.
 * 운영에서 "이 배치 몇 번 죽고 몇 번째에 성공했어요?"의 답이 거기 있다.
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step12.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {RestartableDormantJobConfig.class, TestBatchConfig.class})
@DisplayName("재시작 장부 추적 (연습문제)")
class RestartLedgerExerciseTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        // TODO 1: 회원 21~30 데이터 원상복구 + 장애 스위치 OFF 초기화를 작성하세요
    }

    @AfterEach
    void tearDown() {
        SabotageProcessor.SABOTAGE_ON.set(false);
    }

    @Test
    @DisplayName("장부에 JobInstance 1건 + JobExecution 2건(FAILED, COMPLETED)이 남는다")
    void restart_장부_인스턴스1_실행2() throws Exception {
        // given & when : 같은 파라미터로 [장애 실행 → 복구 → 재시작]을 수행하세요
        //                (example의 1~3막을 참고 — 파라미터가 같아야 합니다!)
        // TODO 2

        // then(1) : BATCH_JOB_INSTANCE에서 JOB_NAME='restartableDormantJob' 행이 1건인지
        // TODO 3
        // then(2) : BATCH_JOB_EXECUTION이 2건이고, STATUS 목록이
        //           FAILED와 COMPLETED를 모두 포함하는지 SQL로 검증하세요
        // TODO 4 (힌트: queryForList("SELECT STATUS FROM ...", String.class))
    }
}
