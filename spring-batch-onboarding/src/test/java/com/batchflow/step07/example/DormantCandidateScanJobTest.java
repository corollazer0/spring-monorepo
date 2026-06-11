package com.batchflow.step07.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.dormant.DormantCandidateScanJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 7 — example] 커서 리더 — DB에서 한 건씩 흘려 읽기
 *
 * 시드 기준값(Step 6에서 봉인): ACTIVE이면서 2025-06-11 이전 로그인 = 정확히 10명.
 * DORMANT 15명과 WITHDRAWN 5명은 WHERE 조건이 걸러낸다 —
 * "읽기 전에 SQL로 거르는 것"과 "읽고 나서 processor로 거르는 것(Step 9)"의 구분 시작.
 */
@SpringBatchTest
@SpringBootTest(classes = {DormantCandidateScanJobConfig.class, TestBatchConfig.class})
@DisplayName("휴면 후보 스캔 (JdbcCursorItemReader)")
class DormantCandidateScanJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    private JobParameters cutoffParams(String cutoffDate) {
        return new JobParametersBuilder()
                .addString("cutoffDate", cutoffDate)
                .toJobParameters();
    }

    @Test
    @DisplayName("기준일 2025-06-11 → 후보 10명을 4+4+2 세 chunk로 읽는다")
    void scanJob_기준일_후보10명() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(cutoffParams("2025-06-11"));

        // then : 카운트 등식으로 전부 증명 (Step 6의 무기)
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount())
                .as("ACTIVE + 기준일 이전 로그인 = 시드의 회원21~30, 10명").isEqualTo(10);
        assertThat(stepExecution.getWriteCount()).isEqualTo(10);
        assertThat(stepExecution.getCommitCount())
                .as("chunk(4): 4+4+2 = 3묶음").isEqualTo(3);
    }

    @Test
    @DisplayName("아주 과거 기준일(2023-01-01) → 후보 0명이어도 Job은 정상 완료")
    void scanJob_과거기준일_빈결과정상() throws Exception {
        // when : 후보가 없는 날 — 운영에서 흔한 "할 일 없는 날"
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(cutoffParams("2023-01-01"));

        // then : 0건은 실패가 아니다. 빈 결과의 정상 종료를 봉인해 둔다
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(0);
        assertThat(stepExecution.getWriteCount()).isEqualTo(0);
    }
}
