package com.batchflow.step02.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.hello.HelloJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * [Batch Step 2 — exercise] 실행 기록을 직접 추적해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 HelloJobTest를 참고)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 힌트: JobExecution 객체에서도(getStepExecutions()), 장부(JdbcTemplate)에서도
 *       같은 사실을 확인할 수 있습니다 — 두 방법 모두 써보는 것이 과제입니다.
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step02.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {HelloJobConfig.class, TestBatchConfig.class})
@DisplayName("Hello Job 실행 기록 (연습문제)")
class HelloJobExerciseTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // TODO 1: 매 테스트 전 장부를 비우세요 (왜 필요한지 설명할 수 있나요?)
    }

    @Test
    @DisplayName("JobExecution 객체에서 Step 실행 정보를 꺼내 검증한다")
    void helloJob_실행후_StepExecution검증() throws Exception {
        // when : Job을 실행하고 JobExecution을 받으세요
        // TODO 2

        // then : jobExecution.getStepExecutions()에서
        //        - Step이 정확히 1개 실행되었고
        //        - 그 이름이 helloStep 인지 검증하세요
        // TODO 3 (힌트: assertThat(...).hasSize(1), .extracting("stepName"))
    }

    @Test
    @DisplayName("두 번 실행하면(서로 다른 파라미터) 장부에 EXECUTION이 2건 쌓인다")
    void helloJob_두번실행_실행기록2건() throws Exception {
        // when : launchJob()을 두 번 호출하세요
        //        (launchJob()은 매번 unique 파라미터를 만들어줘서 두 번째도 실행된다 —
        //         같은 파라미터면 어떻게 되는지는 Step 3에서!)
        // TODO 4

        // then : BATCH_JOB_EXECUTION 테이블의 행 수가 2인지 JdbcTemplate로 확인하세요
        // TODO 5
    }
}
