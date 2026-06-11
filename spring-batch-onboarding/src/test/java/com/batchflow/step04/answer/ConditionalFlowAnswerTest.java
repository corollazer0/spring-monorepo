package com.batchflow.step04.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.flow.ConditionalFlowJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 4 — answer] ConditionalFlowExerciseTest 모범답안
 *
 * 채점 포인트:
 * - STATUS와 EXIT_CODE가 "다른 값"으로 공존함을 장부에서 확인했는가
 *   (이 한 줄이 BatchStatus vs ExitStatus의 결정적 증거)
 * - 파라미터 없는 실행이 거부되는 함정(Step 3)을 unique 파라미터로 피했는가
 */
@SpringBatchTest
@SpringBootTest(classes = {ConditionalFlowJobConfig.class, TestBatchConfig.class})
@DisplayName("BatchStatus vs ExitStatus (모범답안)")
class ConditionalFlowAnswerTest {

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
    @DisplayName("분기용 FAILED는 장부에 STATUS=COMPLETED + EXIT_CODE=FAILED로 남는다")
    void checkStep_실패모드_두상태가다르게기록() throws Exception {
        // given & when (TODO 1 답)
        jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addString("mode", "fail")
                .toJobParameters());

        // then (TODO 2 답) : 같은 행의 두 컬럼이 서로 다른 이야기를 한다!
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT STATUS, EXIT_CODE FROM BATCH_STEP_EXECUTION WHERE STEP_NAME = 'checkStep'");

        assertThat(row.get("STATUS")).isEqualTo("COMPLETED");  // 프레임워크: "정상 종료했다"
        assertThat(row.get("EXIT_CODE")).isEqualTo("FAILED");  // 흐름 제어: "복구 경로로 보내라"
    }

    @Test
    @DisplayName("mode 파라미터가 없으면(on(*) 경로) mainStep으로 흐른다")
    void checkStep_파라미터없음_본처리경로() throws Exception {
        // when (TODO 3 답) : mode 없이 — 단 unique 파라미터로 (같은 빈 파라미터 재실행 거부 방지)
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(
                jobLauncherTestUtils.getUniqueJobParameters());

        // then (TODO 4 답)
        assertThat(jobExecution.getStepExecutions())
                .extracting("stepName")
                .containsExactly("checkStep", "mainStep");
    }
}
