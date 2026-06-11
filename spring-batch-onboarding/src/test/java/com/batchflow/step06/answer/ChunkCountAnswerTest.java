package com.batchflow.step06.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.chunk.FirstChunkJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 6 — answer] ChunkCountExerciseTest 모범답안
 *
 * 채점 포인트:
 * - launchStep(Step 단독 실행)을 활용했는가
 * - 메모리 객체와 장부 양쪽에서 같은 카운트를 확인했는가
 *   (운영에서는 객체가 없다 — 장부를 읽는 능력이 진짜 실력)
 */
@SpringBatchTest
@SpringBootTest(classes = {FirstChunkJobConfig.class, TestBatchConfig.class})
@DisplayName("Chunk 카운트 추적 (모범답안)")
class ChunkCountAnswerTest {

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
    @DisplayName("Step만 단독 실행해도 카운트는 동일하다")
    void firstChunkStep_단독실행_카운트동일() throws Exception {
        // when (TODO 1 답)
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("firstChunkStep");

        // then (TODO 2 답)
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(10);
        assertThat(stepExecution.getFilterCount()).isEqualTo(5);
        assertThat(stepExecution.getWriteCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("같은 카운트가 장부(BATCH_STEP_EXECUTION)에도 영구 기록된다")
    void firstChunkJob_실행후_장부카운트확인() throws Exception {
        // when (TODO 3 답)
        jobLauncherTestUtils.launchJob();

        // then (TODO 4 답)
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT READ_COUNT, FILTER_COUNT, WRITE_COUNT, COMMIT_COUNT " +
                        "FROM BATCH_STEP_EXECUTION WHERE STEP_NAME = 'firstChunkStep'");

        assertThat(((Number) row.get("READ_COUNT")).intValue()).isEqualTo(10);
        assertThat(((Number) row.get("FILTER_COUNT")).intValue()).isEqualTo(5);
        assertThat(((Number) row.get("WRITE_COUNT")).intValue()).isEqualTo(5);
        assertThat(((Number) row.get("COMMIT_COUNT")).intValue()).isEqualTo(4);
    }
}
