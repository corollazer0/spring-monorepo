package com.batchflow.step06.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.chunk.FirstChunkJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * [Batch Step 6 — exercise] 카운트를 객체와 장부 양쪽에서 검증해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 FirstChunkJobTest를 참고)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step06.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {FirstChunkJobConfig.class, TestBatchConfig.class})
@DisplayName("Chunk 카운트 추적 (연습문제)")
class ChunkCountExerciseTest {

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
        // when : launchStep으로 firstChunkStep만 실행하세요
        // TODO 1

        // then : 반환된 JobExecution의 StepExecution에서
        //        read=10 / filter=5 / write=5 를 검증하세요
        // TODO 2
    }

    @Test
    @DisplayName("같은 카운트가 장부(BATCH_STEP_EXECUTION)에도 영구 기록된다")
    void firstChunkJob_실행후_장부카운트확인() throws Exception {
        // when : Job을 실행하세요
        // TODO 3

        // then : BATCH_STEP_EXECUTION에서 STEP_NAME='firstChunkStep' 행의
        //        READ_COUNT / FILTER_COUNT / WRITE_COUNT / COMMIT_COUNT 를
        //        SQL로 조회해 10/5/5/4 인지 검증하세요
        //        (운영 장애 분석 때 보는 게 바로 이 행이다!)
        // TODO 4 (힌트: queryForMap 사용)
    }
}
