package com.batchflow.job.hello;

import com.batchflow.config.TestBatchConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HelloJobConfig 테스트
 *
 * @SpringBatchTest:
 * - JobLauncherTestUtils, JobRepositoryTestUtils 자동 주입
 * - 테스트 환경에서 Job 실행을 쉽게 할 수 있도록 지원
 *
 * @SpringBootTest:
 * - 테스트할 JobConfig와 TestBatchConfig를 명시적으로 로드
 * - 전체 ApplicationContext를 로드하지 않아 테스트 속도 향상
 */
@SpringBatchTest
@SpringBootTest(classes = {HelloJobConfig.class, TestBatchConfig.class})
class HelloJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    /**
     * 각 테스트 실행 전 Job 실행 기록 삭제
     *
     * 이유:
     * - Spring Batch는 동일한 JobParameters로 실행된 Job은 재실행하지 않음
     * - 테스트 간 격리를 위해 이전 실행 기록을 삭제해야 함
     */
    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    /**
     * helloJob 실행 성공 테스트
     *
     * 검증 내용:
     * - Job이 정상적으로 실행되는지
     * - Job 실행 결과가 COMPLETED인지
     */
    @Test
    void helloJob_실행_성공() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
    }

    /**
     * helloStep 단독 실행 테스트
     *
     * 장점:
     * - 특정 Step만 테스트 가능
     * - 디버깅 시 유용
     */
    @Test
    void helloStep_단독실행_성공() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("helloStep");

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
