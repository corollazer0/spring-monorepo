package com.webflow.step07.example;

import com.webflow.order.dao.OrderDao;
import com.webflow.order.domain.Order;
import com.webflow.order.service.OrderCleanupService;
import com.webflow.product.dao.ProductDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * [Web Step 7 — example A] 정리 로직의 협력 검증 (Mockito)
 *
 * 주목: 이 테스트에 @Scheduled도, 시각 대기도 없다 — cutoff가 "파라미터"라서
 * 새벽 3시 로직을 대낮에, 1ms 만에 검증한다. 시각 주입 설계의 보상이다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("미결제 주문 정리 서비스")
class OrderCleanupServiceTest {

    private static final LocalDateTime CUTOFF = LocalDateTime.of(2026, 6, 10, 0, 0);

    @Mock
    private OrderDao orderDao;

    @Mock
    private ProductDao productDao;

    @InjectMocks
    private OrderCleanupService orderCleanupService;

    private Order staleOrder(Long orderId, Long productId, int quantity) {
        return Order.builder()
                .orderId(orderId).productId(productId).quantity(quantity)
                .status(Order.STATUS_PENDING_PAYMENT)
                .build();
    }

    @Test
    @DisplayName("대상 2건: 각각 취소되고, '그 주문의 수량만큼' 재고가 복원된다 (짝 검증)")
    void cancelStaleOrders_2건_취소와복원이짝으로() {
        // given : 수량이 다른 두 건 — 복원량이 뒤섞이면 잡아낼 수 있게
        given(orderDao.findStaleOrders(CUTOFF)).willReturn(Arrays.asList(
                staleOrder(3L, 3L, 1),
                staleOrder(4L, 7L, 2)));

        // when
        int cancelled = orderCleanupService.cancelStaleOrders(CUTOFF);

        // then : 취소↔복원이 주문별로 정확히 짝지어졌는가 (placeOrder의 역연산!)
        assertThat(cancelled).isEqualTo(2);
        then(orderDao).should().updateStatus(eq(3L), eq(Order.STATUS_CANCELLED), isNull());
        then(orderDao).should().updateStatus(eq(4L), eq(Order.STATUS_CANCELLED), isNull());
        then(productDao).should().restoreStock(3L, 1);
        then(productDao).should().restoreStock(7L, 2);
    }

    @Test
    @DisplayName("대상 0건: 아무것도 바꾸지 않고 0을 돌려준다")
    void cancelStaleOrders_빈목록_부수효과없음() {
        // given
        given(orderDao.findStaleOrders(CUTOFF)).willReturn(Collections.emptyList());

        // when
        int cancelled = orderCleanupService.cancelStaleOrders(CUTOFF);

        // then
        assertThat(cancelled).isZero();
        then(orderDao).should(never()).updateStatus(anyLong(), anyString(), any());
        then(productDao).should(never()).restoreStock(anyLong(), anyInt());
    }
}
