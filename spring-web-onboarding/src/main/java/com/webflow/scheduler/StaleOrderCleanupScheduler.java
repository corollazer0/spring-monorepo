package com.webflow.scheduler;

import com.webflow.order.service.OrderCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 미결제 주문 정리 스케줄러 (Step 7) — "시간이 트리거"인 작업.
 *
 * 스케줄러는 얇게: 시각 계산(now - 유예시간)과 위임만 한다.
 * 비즈니스 로직은 전부 OrderCleanupService에 — 그래야 로직을
 * "@Scheduled 없이 직접 호출"로 테스트할 수 있다 (새벽 3시를 기다리지 않는다!).
 *
 * fixedDelay(이전 실행 "종료" 후 N ms) 선택 이유: fixedRate(시작 기준)는
 * 작업이 주기보다 오래 걸리면 실행이 겹친다 — 정리 작업이 밀려도 겹치지 않게.
 */
@Slf4j
@Component
public class StaleOrderCleanupScheduler {

    private final OrderCleanupService orderCleanupService;
    private final int staleMinutes;

    public StaleOrderCleanupScheduler(OrderCleanupService orderCleanupService,
                                      @Value("${app.order.stale-minutes}") int staleMinutes) {
        this.orderCleanupService = orderCleanupService;
        this.staleMinutes = staleMinutes;
    }

    @Scheduled(fixedDelayString = "${app.order.cleanup-interval-millis}")
    public void runCleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(staleMinutes);
        log.info(">>>>> [StaleOrderCleanupScheduler] 정리 시작. cutoff={}", cutoff);

        // 스케줄 스레드에서 예외가 새면 다음 주기 실행까지 위험할 수 있다 — 잡아서 기록
        try {
            orderCleanupService.cancelStaleOrders(cutoff);
        } catch (Exception e) {
            log.error(">>>>> [ERROR] 미결제 주문 정리 실패: {}", e.getMessage(), e);
        }
    }
}
