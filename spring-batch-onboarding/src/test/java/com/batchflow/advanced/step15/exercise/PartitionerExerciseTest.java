package com.batchflow.advanced.step15.exercise;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * [심화 Step 15 — exercise] Partitioner를 "순수 단위"로 테스트해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 발상: MemberIdRangePartitioner의 분할 "산수"는 DB가 필요 없다 —
 * JdbcTemplate만 Mockito로 바꿔치면(TestCraft Step 2!) MIN/MAX를 마음대로 줄 수 있다.
 * 분할 로직의 경계(나누어떨어지지 않는 범위!)를 빠르게 검증하라.
 *
 * 힌트:
 *   JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
 *   given(jdbcTemplate.queryForObject("SELECT MIN(member_id) FROM member", Long.class))
 *           .willReturn(1L);   // MAX도 같은 방식
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step15.md 참고 후 @Disabled를 제거하고 완성하세요")
@DisplayName("ID 범위 파티셔너 (연습문제)")
class PartitionerExerciseTest {

    @Test
    @DisplayName("1~50을 3조각: 1~17 / 18~34 / 35~50 (마지막 조각은 max에서 잘린다)")
    void partition_50을3분할_범위검증() {
        // given : Mock JdbcTemplate이 MIN=1, MAX=50을 돌려주게 하고 파티셔너를 만드세요
        // TODO 1

        // when : partition(3)을 호출하세요
        // TODO 2

        // then : 3개의 파티션이 생기고, partition0=(1,17), partition1=(18,34),
        //        partition2=(35,50)의 minId/maxId를 갖는지 검증하세요
        // TODO 3 (힌트: result.get("partition0").getLong("minId"))
    }

    @Test
    @DisplayName("범위 경계가 연속이고 빈틈이 없다 (이전 max + 1 = 다음 min)")
    void partition_경계_연속성검증() {
        // given & when : MIN=1, MAX=100, gridSize=4로 분할하세요
        // TODO 4

        // then : partition0.maxId + 1 == partition1.minId (이하 연쇄)를 검증하세요
        //        — 빈틈(누락)도 겹침(중복)도 없다는 산수의 증명!
        // TODO 5
    }
}
