package com.batchflow.advanced.step19.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.bulk.BulkArchiveJobConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * [심화 Step 19 — exercise] 커밋 횟수를 공식으로 예측하고 검증해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (힌트: example의 ChunkSizeCommitCountTest)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 질문: chunk 500으로 20,000건이면 커밋이 몇 번일까?
 * 공식: ceil(20000 / 500) = 40 ... 그런데 41일 수도 있다 —
 * 마지막에 "읽을 게 없음"을 확인하는 빈 chunk가 한 번 더 커밋될 수 있어서.
 * 구현 디테일에 강건한 단언(isBetween)이 이 exercise의 포인트다.
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step19.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {BulkArchiveJobConfig.class, TestBatchConfig.class})
@DisplayName("커밋 횟수 예측 (연습문제)")
class CommitCountExerciseTest {

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
        // when : ① chunkSize=500 + 유니크 run 파라미터로 Job을 실행하세요
        //         (⚠️ 헬퍼를 만든다면 JobParameters 반환으로 — Step 18의 함정!)
        // TODO 1

        // then : ② COMPLETED + writeCount가 20,000인지 검증하세요
        // TODO 2

        // then : ③ StepExecution의 commitCount가 40 이상 41 이하인지 검증하세요
        //         (ceil(20000/500)=40, 마지막 빈 chunk 커밋 +1 허용 — isBetween)
        // TODO 3
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
