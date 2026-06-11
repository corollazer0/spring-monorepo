package com.batchflow.advanced.step15.answer;

import com.batchflow.partitioner.MemberIdRangePartitioner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * [심화 Step 15 — answer] PartitionerExerciseTest 모범답안
 *
 * 채점 포인트:
 * - 분할 "산수"를 DB 없이(Mockito) ms 단위로 검증했는가
 * - 경계 연속성(빈틈/겹침 없음)을 일반화해 검증했는가
 */
@DisplayName("ID 범위 파티셔너 (모범답안)")
class PartitionerAnswerTest {

    private MemberIdRangePartitioner partitionerWith(long min, long max) {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        given(jdbcTemplate.queryForObject("SELECT MIN(member_id) FROM member", Long.class))
                .willReturn(min);
        given(jdbcTemplate.queryForObject("SELECT MAX(member_id) FROM member", Long.class))
                .willReturn(max);
        return new MemberIdRangePartitioner(jdbcTemplate);
    }

    @Test
    @DisplayName("1~50을 3조각: 1~17 / 18~34 / 35~50 (마지막 조각은 max에서 잘린다)")
    void partition_50을3분할_범위검증() {
        // given (TODO 1 답)
        MemberIdRangePartitioner partitioner = partitionerWith(1L, 50L);

        // when (TODO 2 답)
        Map<String, ExecutionContext> result = partitioner.partition(3);

        // then (TODO 3 답)
        assertThat(result).hasSize(3);
        assertThat(result.get("partition0").getLong("minId")).isEqualTo(1L);
        assertThat(result.get("partition0").getLong("maxId")).isEqualTo(17L);
        assertThat(result.get("partition1").getLong("minId")).isEqualTo(18L);
        assertThat(result.get("partition1").getLong("maxId")).isEqualTo(34L);
        assertThat(result.get("partition2").getLong("minId")).isEqualTo(35L);
        assertThat(result.get("partition2").getLong("maxId")).isEqualTo(50L); // max에서 잘림!
    }

    @Test
    @DisplayName("범위 경계가 연속이고 빈틈이 없다 (이전 max + 1 = 다음 min)")
    void partition_경계_연속성검증() {
        // given & when (TODO 4 답)
        Map<String, ExecutionContext> result = partitionerWith(1L, 100L).partition(4);

        // then (TODO 5 답) : 연쇄 검증 — 빈틈도 겹침도 없다
        for (int i = 0; i < 3; i++) {
            long prevMax = result.get("partition" + i).getLong("maxId");
            long nextMin = result.get("partition" + (i + 1)).getLong("minId");
            assertThat(nextMin)
                    .as("partition%d의 끝 + 1 = partition%d의 시작", i, i + 1)
                    .isEqualTo(prevMax + 1);
        }
        assertThat(result.get("partition0").getLong("minId")).isEqualTo(1L);
        assertThat(result.get("partition3").getLong("maxId")).isEqualTo(100L);
    }
}
