package com.batchflow.advanced.step14.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.parallel.MultiThreadScanJobConfig;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [심화 Step 14 — example A] Multi-threaded Step — 병렬인데 정확해야 한다
 *
 * 병렬 배치 테스트의 두 기둥:
 * 1. 정확성: 병렬이어도 카운트는 정확히 50 — 중복도 누락도 없다
 *    (이게 깨지면 thread-unsafe reader를 쓴 것 — Step 8의 경고!)
 * 2. 병렬성: 정말 여러 스레드가 일했는가 — 수집된 스레드 이름으로 증명
 */
@SpringBatchTest
@SpringBootTest(classes = {MultiThreadScanJobConfig.class, TestBatchConfig.class})
@DisplayName("멀티스레드 Step")
class MultiThreadScanJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        MultiThreadScanJobConfig.SEEN_THREADS.clear(); // static 수집함 정리 — 격리는 셀프서비스
    }

    @Test
    @DisplayName("병렬이어도 50건 정확히 — 중복도 누락도 없다 (thread-safe reader의 가치)")
    void multiThreadScanJob_병렬실행_카운트정확() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then : 병렬화가 정확성을 해치지 않았다
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(50);
        assertThat(stepExecution.getWriteCount()).isEqualTo(50);
    }

    @Test
    @DisplayName("정말 여러 스레드가 일했다 (batch-mt-* 스레드 2개 이상)")
    void multiThreadScanJob_병렬실행_스레드증거() throws Exception {
        // when
        jobLauncherTestUtils.launchJob();

        // then : 교보재 수집함의 스레드 이름들 — 병렬성의 직접 증거
        assertThat(MultiThreadScanJobConfig.SEEN_THREADS)
                .as("풀(4)에서 최소 2개 이상의 스레드가 chunk를 나눠 들었어야 한다")
                .hasSizeGreaterThanOrEqualTo(2)
                .allSatisfy(name -> assertThat(name).startsWith("batch-mt-"));
    }
}
