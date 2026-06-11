package com.batchflow.step01.answer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 1 — answer] MetadataSchemaExerciseTest 모범답안
 *
 * 채점 포인트:
 * - INFORMATION_SCHEMA로 스키마를 "테스트로" 확인하는 발상을 익혔는가
 * - JOB_KEY의 의미(파라미터 해시)를 주석에서 읽었는가 — Step 3의 복선이다
 */
@SpringBootTest
@DisplayName("메타데이터 스키마 (모범답안)")
class MetadataSchemaAnswerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("BATCH_JOB_INSTANCE에는 JOB_NAME과 JOB_KEY 컬럼이 있다")
    void jobInstance_식별컬럼_존재확인() {
        // when (TODO 1 답)
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_NAME = 'BATCH_JOB_INSTANCE'",
                String.class);

        // then (TODO 2 답)
        assertThat(columns).contains("JOB_NAME", "JOB_KEY");
    }

    @Test
    @DisplayName("BATCH_JOB_EXECUTION에는 상태/시각 컬럼이 있다")
    void jobExecution_상태컬럼_존재확인() {
        // when & then (TODO 3 답)
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_NAME = 'BATCH_JOB_EXECUTION'",
                String.class);

        assertThat(columns).contains("STATUS", "EXIT_CODE", "START_TIME", "END_TIME");
    }
}
