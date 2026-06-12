package com.batchflow.advanced.step17.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.ops.OpsDemoJobConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [심화 Step 17 — example] JobOperator — 코드 없이 Job을 다루는 운영자의 손
 *
 * 지금까지: jobLauncherTestUtils.launchJob() — Job "객체"를 코드로 실행.
 * 운영자는: jobOperator.start("opsDemoJob", "date=...") — "이름 + 문자열"로 실행.
 * 이 간극을 메우는 것이 JobRegistry(이름→Job 전화번호부)다.
 *
 * 백미는 재기동 시나리오 — 새벽 배치 실패의 표준 대응 절차:
 *   실패 확인 → 원인(환경) 복구 → restart(실행ID) → 같은 JobInstance에 성공 실행 추가
 */
@SpringBatchTest
@SpringBootTest(classes = {OpsDemoJobConfig.class, TestBatchConfig.class})
@DisplayName("JobOperator 운영 제어")
class JobOperatorTest {

    private static final String JOB_NAME = "opsDemoJob";

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        OpsDemoJobConfig.BROKEN.set(false);   // 교보재 스위치 초기화 — 격리는 셀프서비스
    }

    @AfterEach
    void tearDown() {
        OpsDemoJobConfig.BROKEN.set(false);   // 정리 의무 (다음 테스트가 장애 상태로 시작하면 안 된다)
    }

    @Test
    @DisplayName("전화번호부 확인: 레지스트리에 Job이 등록되어 있다 (등록 누락 = NoSuchJobException)")
    void getJobNames_레지스트리등록_확인() {
        // when & then : JobRegistryBeanPostProcessor가 일했다는 증거 —
        //               이게 비어 있으면 Job 빈이 멀쩡해도 operator는 아무것도 못 한다!
        assertThat(jobOperator.getJobNames()).contains(JOB_NAME);
    }

    @Test
    @DisplayName("이름 + 문자열 파라미터로 실행하고, 이력(summary)을 들여다본다")
    void start_이름과문자열로_실행과이력() throws Exception {
        // when : 운영자의 방식 — Job 객체 없이 이름과 "key=value" 문자열로
        Long executionId = jobOperator.start(JOB_NAME, "date=2026-06-12,trigger=manual");

        // then-1 : 실행 결과 (동기 런처라 start 반환 시점에 이미 끝나 있다)
        JobExecution execution = jobExplorer.getJobExecution(executionId);
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // then-2 : 운영자가 보는 요약 문자열 — 상태가 그대로 박혀 있다
        assertThat(jobOperator.getSummary(executionId)).contains("COMPLETED");
    }

    @Test
    @DisplayName("새벽 배치 실패의 표준 절차: 실패 → 환경 복구 → restart → 같은 인스턴스에 성공이 쌓인다")
    void restart_실패후복구_재기동성공() throws Exception {
        // given : 환경 장애 속에서 실행 → 실패
        OpsDemoJobConfig.BROKEN.set(true);
        Long failedId = jobOperator.start(JOB_NAME, "date=2026-06-12,trigger=nightly");
        assertThat(jobExplorer.getJobExecution(failedId).getStatus()).isEqualTo(BatchStatus.FAILED);

        // when : 운영자가 환경을 복구하고(스위치 OFF) — 코드 수정 없이 — 재기동
        OpsDemoJobConfig.BROKEN.set(false);
        Long restartedId = jobOperator.restart(failedId);

        // then-1 : 재기동 성공
        assertThat(jobExplorer.getJobExecution(restartedId).getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // then-2 : 두 실행은 "같은 JobInstance"다 — 같은 파라미터의 같은 작업이니까 (Step 3!)
        long instanceId = jobExplorer.getJobExecution(failedId).getJobInstance().getInstanceId();
        assertThat(jobExplorer.getJobExecution(restartedId).getJobInstance().getInstanceId())
                .isEqualTo(instanceId);

        // then-3 : 인스턴스의 실행 이력에 실패 1 + 성공 1이 쌓였다 — 장부는 거짓말하지 않는다
        List<Long> executions = jobOperator.getExecutions(instanceId);
        assertThat(executions).hasSize(2);
    }
}
