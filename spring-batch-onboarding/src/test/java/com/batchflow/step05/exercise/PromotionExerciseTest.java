package com.batchflow.step05.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.context.ShareContextJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * [Batch Step 5 — exercise] 승격되지 "않은" 키의 운명을 추적해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 ShareContextJobTest를 참고)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 배경: countStep은 두 개의 값을 자기 사물함에 넣었다.
 * - targetCount  : PromotionListener의 keys에 있음 → 승격됨
 * - secretNote   : keys에 없음 → Step 사물함에만 남는다!
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step05.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {ShareContextJobConfig.class, TestBatchConfig.class})
@DisplayName("Promotion 범위 (연습문제)")
class PromotionExerciseTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    @DisplayName("keys에 없는 secretNote는 Job 레벨로 승격되지 않는다")
    void promotion_keys에없는키_승격안됨() throws Exception {
        // when : Job을 실행하고 JobExecution을 받으세요
        // TODO 1

        // then(1) : Job 레벨 ExecutionContext에 targetCount는 "있음"을 검증하세요
        // TODO 2 (힌트: jobExecution.getExecutionContext().containsKey(...))

        // then(2) : 같은 곳에 secretNote는 "없음"을 검증하세요 — 승격은 선택한 키만!
        // TODO 3
    }

    @Test
    @DisplayName("secretNote는 countStep의 사물함에는 분명히 들어있다")
    void promotion_step사물함에는존재() throws Exception {
        // when : Job 실행 후 countStep의 StepExecution을 찾으세요
        // TODO 4 (힌트: jobExecution.getStepExecutions().stream().filter(...))

        // then : 그 StepExecution의 ExecutionContext에서
        //        secretNote 값이 "step-only"인지 검증하세요
        // TODO 5
    }
}
