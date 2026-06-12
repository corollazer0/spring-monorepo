package com.batchflow.advanced.step19.example;

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
 * [심화 Step 19 — example B] chunkSize 실험 — 커밋 횟수는 구조, 시간은 환경
 *
 * 같은 2만 건을 chunk 100과 chunk 2000으로 아카이브한다.
 * chunk = 트랜잭션 경계(Step 6)이므로 커밋 수 ≈ ceil(N / chunkSize) —
 * 이것은 머신과 무관한 "구조"라서 단언할 수 있다.
 * 처리 시간은? 로그로 관찰만 한다 — CI 머신의 기분까지 단언하지 마라.
 *
 * (Step 18에서 배운 함정 적용: 헬퍼는 JobParameters 반환 — JobExecution 반환 금지!)
 */
@SpringBatchTest
@SpringBootTest(classes = {BulkArchiveJobConfig.class, TestBatchConfig.class})
@DisplayName("chunk 크기와 커밋 횟수")
class ChunkSizeCommitCountTest {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ChunkSizeCommitCountTest.class);

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
        generateBulkMembers(ROWS);   // example A에서 배운 batchUpdate로 — 수 초가 아니라 수백 ms
    }

    @AfterEach
    void tearDown() {
        cleanBulkTables();
    }

    @Test
    @DisplayName("chunk 100 vs 2000: 결과는 같고, 커밋 횟수는 ~20배 차이다")
    void chunkSize_100vs2000_커밋횟수비교() throws Exception {
        // when-1 : chunk 100 — 커밋 ~200번
        long millis100 = System.nanoTime();
        JobExecution exec100 = jobLauncherTestUtils.launchJob(chunkParams(100));
        millis100 = (System.nanoTime() - millis100) / 1_000_000;
        StepExecution step100 = exec100.getStepExecutions().iterator().next();

        jdbcTemplate.update("DELETE FROM bulk_archive");   // 두 번째 실험을 위한 무대 정리

        // when-2 : chunk 2000 — 커밋 ~10번
        long millis2000 = System.nanoTime();
        JobExecution exec2000 = jobLauncherTestUtils.launchJob(chunkParams(2000));
        millis2000 = (System.nanoTime() - millis2000) / 1_000_000;
        StepExecution step2000 = exec2000.getStepExecutions().iterator().next();

        // then-1 : "일의 양"은 동일 — 변인은 chunk 하나여야 비교가 성립 (Step 16 변인 통제)
        assertThat(exec100.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(exec2000.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(step100.getWriteCount()).isEqualTo(ROWS);
        assertThat(step2000.getWriteCount()).isEqualTo(ROWS);

        // then-2 : 커밋 횟수 = 구조 — ceil(20000/100)=200 vs ceil(20000/2000)=10
        //          (마지막 빈 chunk의 +1 커밋이 있을 수 있어 관계식으로 단언 — 구현 디테일에 강건하게)
        log.info(">>>>> [측정] chunk=100  : 커밋 {}회, {}ms", step100.getCommitCount(), millis100);
        log.info(">>>>> [측정] chunk=2000 : 커밋 {}회, {}ms", step2000.getCommitCount(), millis2000);
        assertThat(step100.getCommitCount()).isGreaterThanOrEqualTo(200);
        assertThat(step2000.getCommitCount()).isLessThanOrEqualTo(11);
        assertThat(step100.getCommitCount())
                .as("커밋 횟수 차이는 머신 무관 — 이게 구조적 단언이다")
                .isGreaterThan(step2000.getCommitCount() * 10);
        // 시간(millis100 vs millis2000)은 단언하지 않는다 — 인메모리 H2의 커밋은 너무 싸서
        // 차이가 노이즈에 묻힐 수 있다. 실서버(디스크 fsync)에서는 이 커밋 수가 곧 시간이 된다.
    }

    /** Step 18의 교훈: 헬퍼는 JobParameters를 반환한다 (JobExecution 반환 = 리스너 오인 함정!) */
    private JobParameters chunkParams(long chunkSize) {
        return new JobParametersBuilder()
                .addLong("chunkSize", chunkSize)
                .addLong("run", System.nanoTime())
                .toJobParameters();
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
