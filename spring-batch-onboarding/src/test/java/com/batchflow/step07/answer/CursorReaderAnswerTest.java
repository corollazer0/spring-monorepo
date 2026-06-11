package com.batchflow.step07.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.dormant.DormantCandidateScanJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 7 — answer] CursorReaderExerciseTest 모범답안
 *
 * 채점 포인트:
 * - 리더의 결과를 독립 SQL로 교차 검증했는가 (자기 증명의 함정 회피)
 * - 경계값(미만 vs 이하)을 예측 → 검증으로 확인했는가
 */
@SpringBatchTest
@SpringBootTest(classes = {DormantCandidateScanJobConfig.class, TestBatchConfig.class})
@DisplayName("커서 리더 교차 검증 (모범답안)")
class CursorReaderAnswerTest {

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

    @Test
    @DisplayName("리더의 READ_COUNT는 같은 조건의 SQL COUNT와 일치한다")
    void cursorReader_READCOUNT_SQL교차검증() throws Exception {
        // given (TODO 1 답)
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addString("cutoffDate", "2025-06-11")
                .toJobParameters());
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();

        // when (TODO 2 답)
        Integer sqlCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member " +
                        "WHERE status = 'ACTIVE' AND last_login_at < '2025-06-11'",
                Integer.class);

        // then (TODO 3 답)
        assertThat(stepExecution.getReadCount()).isEqualTo(sqlCount).isEqualTo(10);
    }

    @Test
    @DisplayName("경계값: 기준일을 정확히 2024-01-15로 주면 0명이다 (미만 조건!)")
    void cursorReader_경계값_예측하고검증() throws Exception {
        // when (TODO 4 답) : 시드의 로그인 시각은 2024-01-15 00:00:00 —
        //                    "< 2024-01-15(00:00)"는 자기 자신을 포함하지 않는다 → 예측: 0명
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addString("cutoffDate", "2024-01-15")
                .toJobParameters());

        // then (TODO 5 답) : 경계 하루 차이가 10명 vs 0명을 가른다 —
        //                    휴면/정산 같은 날짜 기준 배치에서 가장 흔한 사고 지점!
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(0);
    }
}
