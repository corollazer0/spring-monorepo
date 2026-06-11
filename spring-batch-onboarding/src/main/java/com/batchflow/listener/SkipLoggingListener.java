package com.batchflow.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

/**
 * Step 11: Skip 리스너 — "무엇을 왜 건너뛰었는지" 기록한다
 *
 * Skip은 조용히 넘어가라는 뜻이 아니다 — 건너뛴 건은 반드시 기록되어
 * 사후 보정의 근거가 되어야 한다. (기록 없는 Skip = 데이터 증발)
 *
 * StepExecution을 주입받아 ExecutionContext에 누적 기록 → 테스트가 검증 가능.
 * (실무라면 별도 오류 테이블에 INSERT하는 위치)
 */
@Slf4j
@RequiredArgsConstructor
public class SkipLoggingListener implements SkipListener<Integer, Integer> {

    public static final String KEY_SKIPPED_ITEMS = "skippedItems";

    private final StepExecution stepExecution;

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn(">>>>> [SKIP-READ] 읽기 중 건너뜀: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(Integer item, Throwable t) {
        ExecutionContext context = stepExecution.getExecutionContext();
        String existing = context.containsKey(KEY_SKIPPED_ITEMS)
                ? context.getString(KEY_SKIPPED_ITEMS) + ","
                : "";
        context.putString(KEY_SKIPPED_ITEMS, existing + item);
        log.warn(">>>>> [SKIP-PROCESS] item={} 건너뜀 — 사유: {}", item, t.getMessage());
    }

    @Override
    public void onSkipInWrite(Integer item, Throwable t) {
        log.warn(">>>>> [SKIP-WRITE] item={} 쓰기 중 건너뜀: {}", item, t.getMessage());
    }
}
