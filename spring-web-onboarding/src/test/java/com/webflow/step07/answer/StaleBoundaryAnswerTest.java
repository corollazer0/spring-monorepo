package com.webflow.step07.answer;

import com.webflow.order.dao.OrderDao;
import com.webflow.order.domain.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Web Step 7 — answer] StaleBoundaryExerciseTest 모범답안
 *
 * 채점 포인트: 경계 "정확히"와 "1초 뒤" 두 지점을 모두 짚었는가 —
 * 경계 한 점만 테스트하면 <를 <=로 바꿔도 통과해버리는 구멍이 남는다.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("정리 대상 경계 (모범답안)")
class StaleBoundaryAnswerTest {

    @Autowired
    private OrderDao orderDao;

    @Test
    @DisplayName("cutoff가 정확히 09:00이면 09:00 주문은 대상이 아니다 (<는 경계 미포함)")
    void findStaleOrders_경계시각_미포함() {
        // when & then (TODO 1 답)
        assertThat(orderDao.findStaleOrders(LocalDateTime.of(2026, 6, 1, 9, 0))).isEmpty();
    }

    @Test
    @DisplayName("cutoff가 09:00:01이면 09:00 주문만 대상이 된다")
    void findStaleOrders_경계직후_1건() {
        // when (TODO 2 답)
        List<Order> result = orderDao.findStaleOrders(LocalDateTime.of(2026, 6, 1, 9, 0, 1));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderedAt()).isEqualTo(LocalDateTime.of(2026, 6, 1, 9, 0));
    }

    @Test
    @DisplayName("PAID/CANCELLED는 아무리 오래돼도 대상이 아니다")
    void findStaleOrders_상태필터_검증() {
        // when (TODO 3 답)
        List<Order> result = orderDao.findStaleOrders(LocalDateTime.of(2027, 1, 1, 0, 0));

        // then : 미결제 3건뿐 — 상태 필터가 시각 필터와 AND로 작동한다
        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(order ->
                assertThat(order.getStatus()).isEqualTo(Order.STATUS_PENDING_PAYMENT));
    }
}
