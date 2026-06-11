package com.batchflow.step04.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.flow.OddEvenDeciderJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 4 — example B] JobExecutionDecider — 판단 전문 컴포넌트
 *
 * checkStep 방식과의 차이: Decider는 Step이 아니다 —
 * 처리 없이 판단(FlowExecutionStatus)만 반환하고, **장부(STEP_EXECUTION)에도 남지 않는다.**
 * 그래서 실행 Step 목록에 decider는 보이지 않는 것이 정상이다.
 */
@SpringBatchTest
@SpringBootTest(classes = {OddEvenDeciderJobConfig.class, TestBatchConfig.class})
@DisplayName("Decider 분기 (EVEN/ODD)")
class OddEvenDeciderJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    private JobParameters numberParams(long number) {
        return new JobParametersBuilder()
                .addLong("number", number)
                .toJobParameters();
    }

    @Test
    @DisplayName("짝수(4)면 evenStep 경로")
    void oddEvenDeciderJob_짝수_evenStep경로() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(numberParams(4L));

        // then : Decider 자신은 Step 목록에 없다 — 판단만 했으니까
        assertThat(jobExecution.getStepExecutions())
                .extracting("stepName")
                .containsExactly("numberLoadStep", "evenStep");
    }

    @Test
    @DisplayName("홀수(7)면 oddStep 경로")
    void oddEvenDeciderJob_홀수_oddStep경로() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(numberParams(7L));

        // then
        assertThat(jobExecution.getStepExecutions())
                .extracting("stepName")
                .containsExactly("numberLoadStep", "oddStep");
    }
}
