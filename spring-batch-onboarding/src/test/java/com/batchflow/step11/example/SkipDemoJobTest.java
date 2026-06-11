package com.batchflow.step11.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.errorhandling.SkipDemoJobConfig;
import com.batchflow.listener.JobResultLoggingListener;
import com.batchflow.listener.SkipLoggingListener;
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
 * [Batch Step 11 — example A] Skip — 1건의 사고로 전체를 죽이지 않는다
 *
 * 시나리오: 1~10 처리 중 4와 8이 "비정상 데이터" 예외를 던진다.
 * skip 설정 덕분에 그 2건만 격리되고 Job은 COMPLETED로 완주한다.
 *
 * 검증 포인트:
 * 1. 카운트: skip 2 / write 8 — 그리고 ROLLBACK_COUNT의 의미(스킵의 동작 원리)
 * 2. 기록: SkipListener가 "무엇을" 건너뛰었는지 남겼는가 (기록 없는 Skip = 데이터 증발)
 * 3. 알림: JobListener가 최종 상태를 알고 있었는가 (알림 발송 지점)
 */
@SpringBatchTest
@SpringBootTest(classes = {SkipDemoJobConfig.class, TestBatchConfig.class})
@DisplayName("Skip 데모 Job")
class SkipDemoJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    @DisplayName("비정상 2건(4,8)을 건너뛰고 8건을 완주한다 — Job은 COMPLETED!")
    void skipDemoJob_비정상2건_스킵후완주() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then : 예외가 2번 났는데도 Job은 성공이다 — 그것이 skip의 존재 이유
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(10);
        assertThat(stepExecution.getProcessSkipCount())
                .as("4와 8, 두 건이 process 단계에서 격리됐다").isEqualTo(2);
        assertThat(stepExecution.getWriteCount()).isEqualTo(8);

        // skip의 동작 원리 흔적: 예외 → chunk 롤백 → 1건씩 재처리하며 범인 격리.
        // 그래서 롤백이 (스킵 발생 chunk 수만큼) 기록된다
        assertThat(stepExecution.getRollbackCount())
                .as("4가 있던 chunk와 8이 있던 chunk, 각 1회 롤백").isEqualTo(2);
    }

    @Test
    @DisplayName("건너뛴 항목이 SkipListener에 의해 기록된다 (기록 없는 Skip 금지!)")
    void skipDemoJob_스킵항목_기록확인() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then : 어떤 건을 건너뛰었는지 — 사후 보정의 근거
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getExecutionContext()
                .getString(SkipLoggingListener.KEY_SKIPPED_ITEMS)).isEqualTo("4,8");
    }

    @Test
    @DisplayName("JobListener의 afterJob이 최종 상태를 전달받는다 (알림 발송 지점)")
    void skipDemoJob_종료리스너_상태전달() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then
        assertThat(jobExecution.getExecutionContext()
                .getString(JobResultLoggingListener.KEY_NOTIFIED_STATUS)).isEqualTo("COMPLETED");
    }
}
