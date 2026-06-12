package com.batchflow.advanced.step19.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.bulk.BulkArchiveJobConfig;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [심화 Step 19 — answer] CommitCountExerciseTest 모범답안
 *
 * 채점 포인트: 정확값(40)이 아니라 isBetween(40, 41)로 단언했는가 —
 * "구현 디테일(종료 커밋)에 강건한 단언"이 이 exercise의 본전이다.
 * 정확값에 못 박으면 프레임워크 버전업에 테스트가 인질로 잡힌다.
 */
@SpringBatchTest
@SpringBootTest(classes = {BulkArchiveJobConfig.class, TestBatchConfig.class})
@DisplayName("커밋 횟수 예측 (모범답안)")
class CommitCountAnswerTest {

    private static final int ROWS = 20_000;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        cleanBulkTables();
        generateBulkMembers(ROWS);
    }

    @AfterEach
    void tearDown() {
        cleanBulkTables();
    }

    @Test
    @DisplayName("chunk 500 x 20,000건 = 커밋 40회 (+종료 커밋 1회 허용)")
    void chunkSize500_커밋횟수_공식검증() throws Exception {
        // when (TODO 1 답) : 헬퍼 없이 직접 — JobExecution 반환 헬퍼 함정 회피
        JobParameters params = new JobParametersBuilder()
                .addLong("chunkSize", 500L)
                .addLong("run", System.nanoTime())
                .toJobParameters();
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // then (TODO 2 답)
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution stepExecution = execution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getWriteCount()).isEqualTo(ROWS);

        // then (TODO 3 답) : ceil(20000/500)=40 + 종료 커밋 허용 — 구현 디테일에 강건하게
        assertThat(stepExecution.getCommitCount()).isBetween(40, 41);
    }

    private void cleanBulkTables() {
        jdbcTemplate.update("DELETE FROM bulk_archive");
        jdbcTemplate.update("DELETE FROM bulk_member");
    }

    private void generateBulkMembers(int rows) {
        jdbcTemplate.batchUpdate("INSERT INTO bulk_member (member_id, name) VALUES (?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setLong(1, i + 1L);
                        ps.setString(2, "벌크" + (i + 1));
                    }

                    @Override
                    public int getBatchSize() {
                        return rows;
                    }
                });
    }
}
