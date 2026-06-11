package com.batchflow.step03.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.parameters.DailyGreetingJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 3 — answer] JobParametersExerciseTest 모범답안
 *
 * 채점 포인트:
 * - 파라미터가 장부에 남는다는 사실을 SQL로 확인했는가 (운영 추적의 근거)
 * - unique 파라미터가 재실행을 가능케 하는 이유를 JobInstance 관점에서 설명했는가
 */
@SpringBatchTest
@SpringBootTest(classes = {DailyGreetingJobConfig.class, TestBatchConfig.class})
@DisplayName("JobParameters 추적 (모범답안)")
class JobParametersAnswerTest {

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
        // given (TODO 1 답)
        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", "2026-06-11")
                .toJobParameters();

        // when (TODO 2 답)
        jobLauncherTestUtils.launchJob(params);

        // then (TODO 3 답) : "언제 어떤 파라미터로 돌았나"는 장부가 답한다
        String recordedValue = jdbcTemplate.queryForObject(
                "SELECT STRING_VAL FROM BATCH_JOB_EXECUTION_PARAMS WHERE KEY_NAME = 'targetDate'",
                String.class);
        assertThat(recordedValue).isEqualTo("2026-06-11");
    }

    @Test
    @DisplayName("getUniqueJobParameters로는 같은 Job을 몇 번이든 실행할 수 있다")
    void launchJob_unique파라미터_반복실행성공() throws Exception {
        // when (TODO 4 답)
        JobExecution first = jobLauncherTestUtils.launchJob(
                jobLauncherTestUtils.getUniqueJobParameters());
        JobExecution second = jobLauncherTestUtils.launchJob(
                jobLauncherTestUtils.getUniqueJobParameters());

        // then (TODO 5 답) : 파라미터가 매번 다르므로 매번 "새 JobInstance"가 되어
        //                    "성공한 인스턴스 재실행 거부" 규칙에 걸릴 일이 없다
        assertThat(first.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
