package com.batchflow.step02.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.hello.HelloJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 2 — answer] HelloJobExerciseTest 모범답안
 *
 * 채점 포인트:
 * - removeJobExecutions의 필요성을 이해했는가 (메타데이터 격리)
 * - JobExecution 객체와 장부(SQL) 양쪽에서 같은 사실을 검증해봤는가
 */
@SpringBatchTest
@SpringBootTest(classes = {HelloJobConfig.class, TestBatchConfig.class})
@DisplayName("Hello Job 실행 기록 (모범답안)")
class HelloJobAnswerTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // (TODO 1 답) 이전 테스트의 실행 기록이 남으면 카운트 검증이 흔들리고,
        // 같은 파라미터 재실행 거부 규칙(Step 3)에 걸릴 수 있다
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    @DisplayName("JobExecution 객체에서 Step 실행 정보를 꺼내 검증한다")
    void helloJob_실행후_StepExecution검증() throws Exception {
        // when (TODO 2 답)
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then (TODO 3 답)
        assertThat(jobExecution.getStepExecutions())
                .hasSize(1)
                .extracting("stepName")
                .containsExactly("helloStep");
    }

    @Test
    @DisplayName("두 번 실행하면(서로 다른 파라미터) 장부에 EXECUTION이 2건 쌓인다")
    void helloJob_두번실행_실행기록2건() throws Exception {
        // when (TODO 4 답) : launchJob()은 호출마다 unique 파라미터를 생성한다
        jobLauncherTestUtils.launchJob();
        jobLauncherTestUtils.launchJob();

        // then (TODO 5 답)
        Integer executions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION", Integer.class);
        assertThat(executions).isEqualTo(2);
    }
}
