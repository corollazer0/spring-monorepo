package com.batchflow.partitioner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 심화 Step 15: ID 범위 파티셔너 — 데이터를 gridSize 조각으로 나누는 설계자
 *
 * member_id의 MIN/MAX를 조회해 균등 범위로 분할하고,
 * 각 파티션의 ExecutionContext에 minId/maxId를 담아 워커에게 배포한다.
 * 워커 Step은 자기 몫의 범위만 처리한다 — "나눠서 정복".
 */
@Slf4j
@RequiredArgsConstructor
public class MemberIdRangePartitioner implements Partitioner {

    public static final String KEY_MIN_ID = "minId";
    public static final String KEY_MAX_ID = "maxId";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Long min = jdbcTemplate.queryForObject("SELECT MIN(member_id) FROM member", Long.class);
        Long max = jdbcTemplate.queryForObject("SELECT MAX(member_id) FROM member", Long.class);

        Map<String, ExecutionContext> partitions = new HashMap<>();
        long rangeSize = (max - min) / gridSize + 1;

        for (int i = 0; i < gridSize; i++) {
            long start = min + (rangeSize * i);
            long end = Math.min(start + rangeSize - 1, max); // 마지막 조각은 max에서 자른다

            ExecutionContext context = new ExecutionContext();
            context.putLong(KEY_MIN_ID, start);
            context.putLong(KEY_MAX_ID, end);
            partitions.put("partition" + i, context);

            log.info(">>>>> [Partitioner] partition{}: member_id {} ~ {}", i, start, end);
        }
        return partitions;
    }
}
