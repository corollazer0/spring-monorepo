package com.batchflow.advanced.step14.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.parallel.MultiThreadScanJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [심화 Step 14 — answer] ParallelOrderExerciseTest 모범답안
 *
 * 채점 포인트:
 * - 결정적인 것(합계)만 정확히 단정하고, 비결정적인 것(스레드 수)은 "경계"로 단정했는가
 * - "정확히 4 스레드"를 단정하지 않은 이유를 설명했는가
 */
@SpringBatchTest
@SpringBootTest(classes = {MultiThreadScanJobConfig.class, TestBatchConfig.class})
@DisplayName("병렬과 결정성 (모범답안)")
class ParallelOrderAnswerTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        MultiThreadScanJobConfig.SEEN_THREADS.clear();
    }

    @Test
    @DisplayName("두 번 실행해도 카운트는 항상 같다 (결정적인 것)")
    void multiThread_반복실행_카운트결정적() throws Exception {
        // when (TODO 1 답)
        JobExecution first = jobLauncherTestUtils.launchJob(
                jobLauncherTestUtils.getUniqueJobParameters());
        JobExecution second = jobLauncherTestUtils.launchJob(
                jobLauncherTestUtils.getUniqueJobParameters());

        // then (TODO 2 답)
        assertThat(first.getStepExecutions().iterator().next().getReadCount()).isEqualTo(50);
        assertThat(second.getStepExecutions().iterator().next().getReadCount()).isEqualTo(50);
    }

    @Test
    @DisplayName("스레드 사용 개수는 상한(4) 이내다 (단정 가능한 경계)")
    void multiThread_스레드수_상한이내() throws Exception {
        // when (TODO 3 답)
        jobLauncherTestUtils.launchJob();

        // then (TODO 4 답) : "정확히 4"를 단정하면 안 된다 —
        // 작업이 빨리 끝나면 일부 스레드는 일감을 못 받을 수도 있다 (스케줄링은 OS의 영역).
        // 병렬 테스트는 "보장된 경계"만 단정한다: 1 이상(일은 했다), 4 이하(상한 준수)
        assertThat(MultiThreadScanJobConfig.SEEN_THREADS)
                .hasSizeBetween(1, 4);
    }
}
