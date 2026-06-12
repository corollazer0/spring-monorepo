package com.batchflow.advanced.step17.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.ops.OpsDemoJobConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * [심화 Step 17 — exercise] 실행 이력을 직접 조회해보세요 (JobExplorer)
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (힌트: example의 JobOperatorTest + Step 3의 Instance/Execution 구분)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 운영 질문으로 번역하면: "이번 달 이 배치 몇 번 돌았어요? 각각 결과는요?" —
 * 그 답을 주는 읽기 전용 창구가 JobExplorer다 (BATCH_* 테이블의 조회 API).
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step17.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {OpsDemoJobConfig.class, TestBatchConfig.class})
@DisplayName("실행 이력 조회 (연습문제)")
class JobHistoryExerciseTest {

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
        // when : ① jobOperator.start로 "date=2026-06-11", "date=2026-06-12" 두 번 실행하세요
        // TODO 1

        // then : ② jobExplorer.getJobInstances(JOB_NAME, 0, 10)이 2개인지 검증하세요
        //         (파라미터가 다르다 = 다른 작업 = 다른 JobInstance — Step 3의 규칙!)
        // TODO 2

        // then : ③ 각 인스턴스의 getJobExecutions(...)가 1개씩인지 검증하세요
        //         (재시작이 없었으니 인스턴스당 실행은 하나)
        // TODO 3
    }
}
