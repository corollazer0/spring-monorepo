package com.batchflow.step04.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.flow.ConditionalFlowJobConfig;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 4 — example A] 조건 분기 — ExitStatus로 흐름을 가른다
 *
 * 문제 상황: 지금까지 Step은 한 줄로만 흘렀다. 실무 요구는
 * "검증이 실패하면 복구 Step으로, 성공하면 본처리 Step으로".
 *
 * 핵심 검증 포인트:
 * 1. 어느 경로를 탔는지는 "실행된 Step 이름 목록"으로 증명한다
 * 2. 🚨 checkStep이 ExitStatus=FAILED여도 Job의 최종 상태는 COMPLETED다!
 *    — ExitStatus는 흐름 제어 코드일 뿐, Job의 성패가 아니다 (Step 2 복선 회수)
 */
@SpringBatchTest
@SpringBootTest(classes = {ConditionalFlowJobConfig.class, TestBatchConfig.class})
@DisplayName("조건 분기 Flow (on/to)")
class ConditionalFlowJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    private JobParameters modeParams(String mode) {
        return new JobParametersBuilder()
                .addString("mode", mode)
                .toJobParameters();
    }

    @Test
    @DisplayName("검증 통과(mode=ok) → checkStep 다음 mainStep 경로를 탄다")
    void conditionalFlowJob_정상모드_본처리경로() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(modeParams("ok"));

        // then : 경로 증명 = 실행된 Step 이름의 순서
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getStepExecutions())
                .extracting("stepName")
                .containsExactly("checkStep", "mainStep"); // recoveryStep은 실행되지 않았다
    }

    @Test
    @DisplayName("검증 실패(mode=fail) → recoveryStep 경로, 그런데 Job은 COMPLETED!")
    void conditionalFlowJob_실패모드_복구경로_그러나Job은성공() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(modeParams("fail"));

        // then(1) : 복구 경로를 탔다
        assertThat(jobExecution.getStepExecutions())
                .extracting("stepName")
                .containsExactly("checkStep", "recoveryStep");

        // then(2) 🚨 핵심 : "FAILED로 분기"했는데 Job의 최종 BatchStatus는 COMPLETED다.
        //          ExitStatus(FAILED)는 행선지를 정하는 코드였을 뿐 —
        //          분기 끝에 정의된 경로로 잘 흘러갔으니 Job은 성공이다.
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
