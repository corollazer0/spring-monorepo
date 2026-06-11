package com.batchflow.step03.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.parameters.DailyGreetingJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [Batch Step 3 — example] JobParameters & JobInstance — 재실행 거부 사건의 전말
 *
 * 문제 상황: Step 2의 launchJob()은 매번 몰래 unique 파라미터를 만들어줬다.
 * 운영처럼 "2026-06-11 기준으로"라고 같은 파라미터를 직접 주면? — 두 번째는 거부된다!
 *
 * 이것은 버그가 아니라 핵심 설계다:
 * JobInstance = Job 이름 + 파라미터(JOB_KEY 해시, Step 1의 복선!).
 * "성공한 인스턴스의 재실행 거부" = 같은 날짜 정산이 두 번 도는 사고의 방지 장치.
 */
@SpringBatchTest
@SpringBootTest(classes = {DailyGreetingJobConfig.class, TestBatchConfig.class})
@DisplayName("JobParameters와 JobInstance")
class DailyGreetingJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    private JobParameters paramsOf(String targetDate) {
        return new JobParametersBuilder()
                .addString("targetDate", targetDate)
                .toJobParameters();
    }

    @Test
    @DisplayName("@JobScope Late Binding — 파라미터가 실행 시점의 Step에 주입된다")
    void dailyGreetingJob_파라미터전달_Step에주입() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(paramsOf("2026-06-11"));

        // then : Tasklet이 주입받아 ExecutionContext에 남긴 값으로 주입을 증명
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getExecutionContext().getString("greetedDate"))
                .isEqualTo("2026-06-11");
    }

    /**
     * 🚨 이 Step의 핵심 사건. 같은 파라미터 = 같은 JobInstance = 이미 성공 = 재실행 거부.
     */
    @Test
    @DisplayName("같은 파라미터로 성공한 Job의 재실행은 거부된다 (중복 정산 방지 장치!)")
    void dailyGreetingJob_같은파라미터재실행_거부() throws Exception {
        // given : 같은 기준일로 한 번 성공시켜 둔다
        jobLauncherTestUtils.launchJob(paramsOf("2026-06-11"));

        // when & then : 두 번째 실행은 예외 — "이미 그 날짜는 성공적으로 처리됐다"
        assertThatThrownBy(() -> jobLauncherTestUtils.launchJob(paramsOf("2026-06-11")))
                .isInstanceOf(JobInstanceAlreadyCompleteException.class);
    }

    @Test
    @DisplayName("파라미터가 다르면 다른 JobInstance — 장부에 인스턴스 2건")
    void dailyGreetingJob_다른파라미터_별도JobInstance() throws Exception {
        // when : 어제와 오늘 — 운영에서 매일 도는 배치의 모습
        jobLauncherTestUtils.launchJob(paramsOf("2026-06-10"));
        jobLauncherTestUtils.launchJob(paramsOf("2026-06-11"));

        // then : JobInstance가 2건 (JOB_KEY = 파라미터 해시가 다르므로)
        Integer instances = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BATCH_JOB_INSTANCE WHERE JOB_NAME = 'dailyGreetingJob'",
                Integer.class);
        assertThat(instances).isEqualTo(2);
    }
}
