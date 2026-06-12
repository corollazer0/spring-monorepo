package com.batchflow.advanced.step17.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.ops.OpsDemoJobConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [심화 Step 17 — answer] JobHistoryExerciseTest 모범답안
 *
 * 채점 포인트: Instance(작업)와 Execution(시도)을 구분해 셌는가 —
 * Step 3에서 배운 두 단어가 운영 이력 조회의 어휘 그 자체다.
 */
@SpringBatchTest
@SpringBootTest(classes = {OpsDemoJobConfig.class, TestBatchConfig.class})
@DisplayName("실행 이력 조회 (모범답안)")
class JobHistoryAnswerTest {

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
        OpsDemoJobConfig.BROKEN.set(false);
    }

    @AfterEach
    void tearDown() {
        OpsDemoJobConfig.BROKEN.set(false);
    }

    @Test
    @DisplayName("파라미터가 다르면 인스턴스가 쌓인다 — 2일치 실행 = 인스턴스 2개, 실행 각 1개")
    void getJobInstances_이틀치실행_이력조회() throws Exception {
        // when (TODO 1 답) : 날짜 파라미터가 다른 두 번의 "일일 배치"
        jobOperator.start(JOB_NAME, "date=2026-06-11");
        jobOperator.start(JOB_NAME, "date=2026-06-12");

        // then (TODO 2 답) : 다른 파라미터 = 다른 작업 = 다른 JobInstance
        List<JobInstance> instances = jobExplorer.getJobInstances(JOB_NAME, 0, 10);
        assertThat(instances).hasSize(2);

        // then (TODO 3 답) : 재시작이 없었으니 작업(Instance)당 시도(Execution)는 하나
        instances.forEach(instance ->
                assertThat(jobExplorer.getJobExecutions(instance)).hasSize(1));
    }
}
