package com.batchflow.advanced.step14.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.parallel.ParallelFlowJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [심화 Step 14 — example B] Parallel Flow(split) — 독립 Step들의 동시 실행
 */
@SpringBatchTest
@SpringBootTest(classes = {ParallelFlowJobConfig.class, TestBatchConfig.class})
@DisplayName("병렬 Flow (split)")
class ParallelFlowJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    @DisplayName("두 Flow의 Step이 모두 실행되고, 서로 다른 flow-* 스레드를 탔다")
    void parallelFlowJob_분기실행_서로다른스레드() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then(1) : 두 Step 모두 완료
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getStepExecutions())
                .extracting("stepName")
                .containsExactlyInAnyOrder("memberStatStep", "transactionStatStep");
        // containsExactlyInAnyOrder인 이유: 병렬이라 "완료 순서"는 매번 다를 수 있다!

        // then(2) : 각 Step이 남긴 스레드 증거 — 전용 flow-* 스레드, 그리고 서로 다르다
        Map<String, String> threadByStep = jobExecution.getStepExecutions().stream()
                .collect(Collectors.toMap(
                        StepExecution::getStepName,
                        step -> step.getExecutionContext().getString("workerThread")));

        assertThat(threadByStep.values())
                .allSatisfy(thread -> assertThat(thread).startsWith("flow-"));
        assertThat(threadByStep.get("memberStatStep"))
                .isNotEqualTo(threadByStep.get("transactionStatStep"));
    }
}
