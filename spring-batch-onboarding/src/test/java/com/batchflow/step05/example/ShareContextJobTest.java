package com.batchflow.step05.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.context.ShareContextJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 5 — example] ExecutionContext — Step 간 데이터 공유의 공식 통로
 *
 * 문제 상황: countStep이 집계한 건수를 reportStep이 써야 한다.
 * 멤버 변수? static? — 멀티스레드와 재시작에서 전부 깨진다.
 *
 * 공식 통로: Step ExecutionContext(개인 사물함) → PromotionListener(승격) →
 * Job ExecutionContext(공용 게시판) → 다음 Step이 읽는다.
 *
 * 보너스: 이 모든 것이 장부(BATCH_JOB_EXECUTION_CONTEXT)에 직렬화되어 저장된다 —
 * 재시작(Step 12) 시 상태가 복원되는 비밀이 바로 이것이다.
 */
@SpringBatchTest
@SpringBootTest(classes = {ShareContextJobConfig.class, TestBatchConfig.class})
@DisplayName("ExecutionContext 공유 (Promotion)")
class ShareContextJobTest {

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
    @DisplayName("countStep의 값이 Job 레벨로 승격된다")
    void shareContextJob_실행후_Job컨텍스트에승격() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then : PromotionListener가 keys에 지정한 targetCount를 공용 게시판에 올렸다
        assertThat(jobExecution.getExecutionContext()
                .getInt(ShareContextJobConfig.KEY_TARGET_COUNT)).isEqualTo(42);
    }

    @Test
    @DisplayName("reportStep이 앞 Step의 값을 실제로 전달받았다")
    void shareContextJob_실행후_reportStep수신확인() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then : reportStep이 자기 사물함에 남긴 "받았다" 증거를 확인
        StepExecution reportStep = jobExecution.getStepExecutions().stream()
                .filter(step -> "reportStep".equals(step.getStepName()))
                .findFirst().orElseThrow(() -> new AssertionError("reportStep 미실행"));

        assertThat(reportStep.getExecutionContext().getInt("reportedCount")).isEqualTo(42);
    }

    @Test
    @DisplayName("ExecutionContext는 장부에 직렬화되어 저장된다 (재시작 복원의 비밀)")
    void shareContextJob_실행후_장부직렬화확인() throws Exception {
        // when
        jobLauncherTestUtils.launchJob();

        // then : Job 레벨 컨텍스트가 BATCH_JOB_EXECUTION_CONTEXT에 문자열로 남아있다
        String serialized = jdbcTemplate.queryForObject(
                "SELECT SHORT_CONTEXT FROM BATCH_JOB_EXECUTION_CONTEXT", String.class);
        assertThat(serialized).contains(ShareContextJobConfig.KEY_TARGET_COUNT);
    }
}
