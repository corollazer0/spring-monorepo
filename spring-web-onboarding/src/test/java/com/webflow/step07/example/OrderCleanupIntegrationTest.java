package com.webflow.step07.example;

import com.webflow.order.dao.OrderDao;
import com.webflow.order.domain.Order;
import com.webflow.order.service.OrderCleanupService;
import com.webflow.product.dao.ProductDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Web Step 7 — example B] 정리 작업 통합 검증 — 진짜 DB에서 끝까지
 *
 * Mockito(example A)는 "협력"을, 여기는 "결과 상태"를 검증한다:
 * 시드의 오래된 미결제 2건(주문 3·4)이 실제로 CANCELLED가 되고
 * 재고가 복원되며, 건드리면 안 되는 것들(PAID/최근 미결제)은 그대로인가.
 *
 * @Transactional: 데이터를 바꾸는 통합 테스트 — 끝나면 롤백되어 시드가 복구된다.
 */
@SpringBootTest
@Transactional
@DisplayName("미결제 주문 정리 (통합)")
class OrderCleanupIntegrationTest {

    /** 시드 기준: 2026-06-01 미결제 2건만 이 cutoff 이전이다 */
    private static final LocalDateTime CUTOFF = LocalDateTime.of(2026, 6, 10, 0, 0);

    @Autowired
    private OrderCleanupService orderCleanupService;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private ProductDao productDao;

    @Test
    @DisplayName("오래된 미결제 2건만 취소 + 재고 복원 — 나머지는 무사하다")
    void cancelStaleOrders_시드기준_2건정리() {
        // given : 시드 상태 확인 (저소음 키보드=상품3 재고 10)
        assertThat(productDao.findById(3L).getStock()).isEqualTo(10);

        // when
        int cancelled = orderCleanupService.cancelStaleOrders(CUTOFF);

        // then-1 : 대상 2건이 취소됐다
        assertThat(cancelled).isEqualTo(2);
        assertThat(orderDao.findById(3L).getStatus()).isEqualTo(Order.STATUS_CANCELLED);
        assertThat(orderDao.findById(4L).getStatus()).isEqualTo(Order.STATUS_CANCELLED);

        // then-2 : 잠겨 있던 재고가 돌아왔다 (주문 3=상품3 1개, 주문 4=상품7 1개)
        assertThat(productDao.findById(3L).getStock()).isEqualTo(11);
        assertThat(productDao.findById(7L).getStock()).isEqualTo(11);

        // then-3 : 건드리면 안 되는 것들 — PAID와 "최근" 미결제는 그대로
        assertThat(orderDao.findById(1L).getStatus()).isEqualTo(Order.STATUS_PAID);
        assertThat(orderDao.findById(5L).getStatus()).isEqualTo(Order.STATUS_PENDING_PAYMENT);

        // then-4 : 한 번 더 돌려도 대상이 없다 (멱등 — 같은 주문을 두 번 취소하지 않는다)
        assertThat(orderCleanupService.cancelStaleOrders(CUTOFF)).isZero();
    }
}
