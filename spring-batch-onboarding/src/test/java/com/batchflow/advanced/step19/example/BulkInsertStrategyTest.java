package com.batchflow.advanced.step19.example;

import com.batchflow.config.TestBatchConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [심화 Step 19 — example A] 대량 데이터는 "만드는 것"부터 기술이다
 *
 * 같은 5,000건을 두 방식으로 만든다:
 *   건별 INSERT  : 5,000번의 SQL 왕복 — DB가 아니라 "왕복"이 비싸다
 *   batchUpdate  : 한 번에 묶어 전송 — JDBC batch (Step 16 writer 튜닝의 원형)
 *
 * 측정 원칙 (이 Step의 본전):
 *   시간은 머신마다 다르다 → 로그로 "관찰"만 한다
 *   단언은 결정적인 것만 → 건수(구조) + 방향성(상대 비교, 구조적 차이가 큰 것만)
 */
@SpringBootTest(classes = TestBatchConfig.class)
@DisplayName("대량 생성 전략 비교")
class BulkInsertStrategyTest {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(BulkInsertStrategyTest.class);

    private static final int ROWS = 5_000;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanBulkTables() {
        jdbcTemplate.update("DELETE FROM bulk_archive");
        jdbcTemplate.update("DELETE FROM bulk_member");
    }

    @Test
    @DisplayName("건별 INSERT vs batchUpdate — 왕복 횟수가 차이를 만든다")
    void 대량생성_건별vs배치_비교() {
        // when-1 : 건별 — 5,000번의 왕복
        long perRowMillis = measure(() -> {
            for (int i = 1; i <= ROWS; i++) {
                jdbcTemplate.update("INSERT INTO bulk_member (member_id, name) VALUES (?, ?)",
                        i, "벌크" + i);
            }
        });
        assertThat(countBulkMembers()).isEqualTo(ROWS);
        jdbcTemplate.update("DELETE FROM bulk_member");

        // when-2 : batchUpdate — 묶어서 전송
        long batchMillis = measure(() ->
                jdbcTemplate.batchUpdate("INSERT INTO bulk_member (member_id, name) VALUES (?, ?)",
                        new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement ps, int i) throws SQLException {
                                ps.setLong(1, i + 1L);
                                ps.setString(2, "벌크" + (i + 1));
                            }

                            @Override
                            public int getBatchSize() {
                                return ROWS;
                            }
                        }));

        // then : 정확성(구조)은 단언, 시간은 관찰
        assertThat(countBulkMembers()).isEqualTo(ROWS);
        log.info(">>>>> [측정] 건별 INSERT {}건: {}ms / batchUpdate: {}ms (x{} 배)",
                ROWS, perRowMillis, batchMillis,
                batchMillis == 0 ? "∞" : String.format("%.1f", (double) perRowMillis / batchMillis));

        // 인메모리 H2조차 왕복 5,000번의 비용은 구조적이다 — 방향성만 단언
        assertThat(batchMillis)
                .as("묶어 보내기가 건별 왕복보다 느리다면 측정이나 환경을 의심하라")
                .isLessThan(perRowMillis);
    }

    private long measure(Runnable work) {
        long start = System.nanoTime();
        work.run();
        return (System.nanoTime() - start) / 1_000_000;
    }

    private Integer countBulkMembers() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bulk_member", Integer.class);
    }
}
