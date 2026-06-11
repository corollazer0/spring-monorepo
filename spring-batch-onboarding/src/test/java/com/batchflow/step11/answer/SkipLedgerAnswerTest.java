package com.batchflow.step11.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.errorhandling.SkipDemoJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 11 — answer] SkipLedgerExerciseTest 모범답안
 *
 * 채점 포인트:
 * - 카운트 등식(READ = WRITE + SKIP)을 장부에서 완성했는가
 * - "성공인데 skip이 있는 실행"의 모니터링 사각지대를 인식했는가
 */
@SpringBatchTest
@SpringBootTest(classes = {SkipDemoJobConfig.class, TestBatchConfig.class})
@DisplayName("Skip 장부 추적 (모범답안)")
class SkipLedgerAnswerTest {

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
    @DisplayName("장부에서 카운트 등식을 완성하라: READ = WRITE + PROCESS_SKIP")
    void skipDemoJob_장부_카운트등식() throws Exception {
        // when (TODO 1 답)
        jobLauncherTestUtils.launchJob();

        // then (TODO 2 답)
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT READ_COUNT, WRITE_COUNT, PROCESS_SKIP_COUNT, ROLLBACK_COUNT " +
                        "FROM BATCH_STEP_EXECUTION WHERE STEP_NAME = 'skipDemoStep'");

        int read = ((Number) row.get("READ_COUNT")).intValue();
        int write = ((Number) row.get("WRITE_COUNT")).intValue();
        int skip = ((Number) row.get("PROCESS_SKIP_COUNT")).intValue();

        assertThat(read).isEqualTo(10);
        assertThat(write).isEqualTo(8);
        assertThat(skip).isEqualTo(2);
        assertThat(read).isEqualTo(write + skip); // 등식!
        assertThat(((Number) row.get("ROLLBACK_COUNT")).intValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("EXIT_CODE는 COMPLETED지만 skip이 있었다 — 운영 모니터링의 사각지대")
    void skipDemoJob_성공이지만skip존재_구분() throws Exception {
        // when (TODO 3 답)
        jobLauncherTestUtils.launchJob();

        // then(1) (TODO 4 답)
        String exitCode = jdbcTemplate.queryForObject(
                "SELECT EXIT_CODE FROM BATCH_STEP_EXECUTION WHERE STEP_NAME = 'skipDemoStep'",
                String.class);
        assertThat(exitCode).isEqualTo("COMPLETED");

        // then(2) (TODO 5 답) : 성공인데 skip > 0 — 상태만 보는 모니터링은 이걸 놓친다.
        // 아이디어: PROCESS_SKIP_COUNT > 0 이면 별도 경고 알림 / SkipListener에서
        // 오류 테이블 INSERT 후 일일 리포트 — "성공"과 "완전한 성공"을 구분하라
        Integer skipCount = jdbcTemplate.queryForObject(
                "SELECT PROCESS_SKIP_COUNT FROM BATCH_STEP_EXECUTION WHERE STEP_NAME = 'skipDemoStep'",
                Integer.class);
        assertThat(skipCount).isGreaterThan(0);
    }
}
