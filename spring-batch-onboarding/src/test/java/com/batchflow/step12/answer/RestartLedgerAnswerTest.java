package com.batchflow.step12.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.restart.RestartableDormantJobConfig;
import com.batchflow.processor.SabotageProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 12 — answer] RestartLedgerExerciseTest 모범답안
 *
 * 채점 포인트:
 * - 두 실행에 "같은 파라미터"를 썼는가 (다르면 별개 인스턴스 2개가 되어버린다!)
 * - 1:N(인스턴스:실행)을 장부에서 직접 확인했는가
 */
@SpringBatchTest
@SpringBootTest(classes = {RestartableDormantJobConfig.class, TestBatchConfig.class})
@DisplayName("재시작 장부 추적 (모범답안)")
class RestartLedgerAnswerTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        restoreMembers(); // (TODO 1 답)
        SabotageProcessor.SABOTAGE_ON.set(false);
    }

    @AfterEach
    void tearDown() {
        SabotageProcessor.SABOTAGE_ON.set(false);
        restoreMembers(); // 내가 어지럽힌 것은 내가 치운다
    }

    private void restoreMembers() {
        jdbcTemplate.update("UPDATE member SET status = 'ACTIVE', dormant_at = NULL " +
                "WHERE member_id BETWEEN 21 AND 30");
    }

    @Test
    @DisplayName("장부에 JobInstance 1건 + JobExecution 2건(FAILED, COMPLETED)이 남는다")
    void restart_장부_인스턴스1_실행2() throws Exception {
        // given & when (TODO 2 답) : 같은 파라미터 객체로 장애 → 복구 → 재시작
        JobParameters params = new JobParametersBuilder()
                .addString("cutoffDate", "2025-06-11")
                .addString("dormantAt", "2026-06-11T03:00:00")
                .toJobParameters();

        SabotageProcessor.SABOTAGE_ON.set(true);
        jobLauncherTestUtils.launchJob(params);   // 1차: FAILED
        SabotageProcessor.SABOTAGE_ON.set(false);
        jobLauncherTestUtils.launchJob(params);   // 2차: 재시작 → COMPLETED

        // then(1) (TODO 3 답) : 논리적 실행 단위는 하나
        Integer instances = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_INSTANCE WHERE JOB_NAME = 'restartableDormantJob'",
                Integer.class);
        assertThat(instances).isEqualTo(1);

        // then(2) (TODO 4 답) : 그 하나의 인스턴스에 시도가 둘 — 1:N의 실증!
        List<String> statuses = jdbcTemplate.queryForList(
                "SELECT STATUS FROM BATCH_JOB_EXECUTION ORDER BY JOB_EXECUTION_ID",
                String.class);
        assertThat(statuses).containsExactly("FAILED", "COMPLETED");
    }
}
