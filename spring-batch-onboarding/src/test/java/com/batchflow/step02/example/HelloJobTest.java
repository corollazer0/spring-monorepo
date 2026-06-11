package com.batchflow.step02.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.hello.HelloJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 2 — example] 가장 작은 배치: Job → Step → Tasklet
 *
 * 문제 상황: 판(Step 1)은 깔렸는데 Job이 하나도 없다.
 * 가장 작은 구조로 "배치 한 번의 실행"이 무엇인지 해부한다.
 *
 * 구조: helloJob (실행 단위) ─ helloStep (작업 단위) ─ Tasklet (실제 코드 한 덩이)
 *
 * 이 테스트가 보여주는 것:
 * 1. @SpringBatchTest + @SpringBootTest(classes=...) — 필요한 JobConfig만 띄우는 배치 테스트 표준
 * 2. JobLauncherTestUtils.launchJob() — 테스트에서 Job을 실행하는 표준 방법
 * 3. removeJobExecutions() — 메타데이터 격리 (TestCraft의 롤백에 해당하는 장치)
 * 4. 실행 한 번이 장부(BATCH_*)에 정확히 어떤 기록을 남기는지 — Step 1과의 연결!
 */
@SpringBatchTest
@SpringBootTest(classes = {HelloJobConfig.class, TestBatchConfig.class})
@DisplayName("Hello Job (Job/Step/Tasklet)")
class HelloJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Spring Batch는 "같은 파라미터의 성공한 Job"을 다시 실행하지 않는다(Step 3에서 상세히).
     * 이전 테스트의 실행 기록이 남아있으면 그 규칙에 걸리므로, 매 테스트 전 장부를 비운다.
     */
    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    @DisplayName("helloJob 실행이 COMPLETED로 끝난다")
    void helloJob_실행_성공() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then : BatchStatus(프레임워크가 본 상태)와 ExitStatus(흐름 제어용 코드) 둘 다 확인
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("helloStep만 단독 실행할 수도 있다 (디버깅 무기)")
    void helloStep_단독실행_성공() throws Exception {
        // when : Job 전체가 아니라 특정 Step만 — 복잡한 Job에서 문제 Step을 격리할 때 유용
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("helloStep");

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    /**
     * Step 1에서 본 장부가 "실제로 기록되는 순간". 실행 한 번 = INSTANCE 1 + EXECUTION 1 + STEP 1.
     */
    @Test
    @DisplayName("실행 한 번이 장부에 정확히 한 세트의 기록을 남긴다")
    void helloJob_실행후_메타데이터기록확인() throws Exception {
        // when
        jobLauncherTestUtils.launchJob();

        // then : 논리 단위 1건 + 실행 시도 1건 + Step 기록 1건
        Integer instances = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_INSTANCE WHERE JOB_NAME = 'helloJob'", Integer.class);
        Integer executions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION", Integer.class);
        String stepStatus = jdbcTemplate.queryForObject(
                "SELECT STATUS FROM BATCH_STEP_EXECUTION WHERE STEP_NAME = 'helloStep'", String.class);

        assertThat(instances).isEqualTo(1);
        assertThat(executions).isEqualTo(1);
        assertThat(stepStatus).isEqualTo("COMPLETED");
    }
}
