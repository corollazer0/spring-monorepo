package com.batchflow.step05.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.context.ShareContextJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 5 — answer] PromotionExerciseTest 모범답안
 *
 * 채점 포인트:
 * - "승격은 keys에 지정한 것만"이라는 규칙을 양쪽(있음/없음)으로 증명했는가
 * - Step 사물함과 Job 게시판의 스코프 차이를 코드로 확인했는가
 */
@SpringBatchTest
@SpringBootTest(classes = {ShareContextJobConfig.class, TestBatchConfig.class})
@DisplayName("Promotion 범위 (모범답안)")
class PromotionAnswerTest {

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
        // when (TODO 1 답)
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then(1) (TODO 2 답) : 지정한 키는 공용 게시판에 있다
        assertThat(jobExecution.getExecutionContext()
                .containsKey(ShareContextJobConfig.KEY_TARGET_COUNT)).isTrue();

        // then(2) (TODO 3 답) : 지정하지 않은 키는 올라가지 않았다 — 승격은 화이트리스트!
        assertThat(jobExecution.getExecutionContext()
                .containsKey(ShareContextJobConfig.KEY_SECRET_NOTE)).isFalse();
    }

    @Test
    @DisplayName("secretNote는 countStep의 사물함에는 분명히 들어있다")
    void promotion_step사물함에는존재() throws Exception {
        // when (TODO 4 답)
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        StepExecution countStep = jobExecution.getStepExecutions().stream()
                .filter(step -> "countStep".equals(step.getStepName()))
                .findFirst().orElseThrow(() -> new AssertionError("countStep 미실행"));

        // then (TODO 5 답)
        assertThat(countStep.getExecutionContext()
                .getString(ShareContextJobConfig.KEY_SECRET_NOTE)).isEqualTo("step-only");
    }
}
