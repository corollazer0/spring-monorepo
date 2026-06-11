package com.webflow.step07.example;

import com.webflow.order.service.OrderCleanupService;
import com.webflow.scheduler.StaleOrderCleanupScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * [Web Step 7 — example C] 스케줄러는 "얇음"을 검증한다
 *
 * @Scheduled 트리거 자체(10분마다 도는가)는 Spring의 책임이라 우리는 안 믿어볼
 * 필요가 없다. 우리가 봉인할 것은 우리 코드:
 *   ① cutoff 계산이 맞는가 (now - stale-minutes)
 *   ② 예외가 새지 않는가 (스케줄 스레드 보호)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("정리 스케줄러")
class StaleOrderCleanupSchedulerTest {

    @Mock
    private OrderCleanupService orderCleanupService;

    @Test
    @DisplayName("cutoff = 지금 - 30분 — Captor로 계산을 검증한다")
    void runCleanup_cutoff계산_검증() {
        // given : 유예 30분짜리 스케줄러를 직접 생성 (Spring 불필요!)
        StaleOrderCleanupScheduler scheduler =
                new StaleOrderCleanupScheduler(orderCleanupService, 30);
        LocalDateTime before = LocalDateTime.now();

        // when : @Scheduled를 기다리지 않는다 — 메서드를 그냥 부른다
        scheduler.runCleanup();

        // then : now()는 고정할 수 없으니 "범위"로 단언한다
        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        then(orderCleanupService).should().cancelStaleOrders(captor.capture());

        LocalDateTime cutoff = captor.getValue();
        assertThat(cutoff).isAfterOrEqualTo(before.minusMinutes(30))
                .isBeforeOrEqualTo(after.minusMinutes(30));
    }

    @Test
    @DisplayName("정리 작업이 터져도 스케줄러는 예외를 새게 하지 않는다")
    void runCleanup_서비스예외_삼키고기록() {
        // given
        StaleOrderCleanupScheduler scheduler =
                new StaleOrderCleanupScheduler(orderCleanupService, 30);
        given(orderCleanupService.cancelStaleOrders(any()))
                .willThrow(new RuntimeException("DB 일시 장애"));

        // when & then : 예외가 호출자(스케줄 스레드)로 전파되지 않는다
        assertThatCode(scheduler::runCleanup).doesNotThrowAnyException();
    }
}
