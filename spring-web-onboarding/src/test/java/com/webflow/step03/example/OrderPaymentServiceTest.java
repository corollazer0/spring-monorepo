package com.webflow.step03.example;

import com.webflow.common.exception.BusinessException;
import com.webflow.common.exception.OrderNotFoundException;
import com.webflow.common.exception.PaymentDeclinedException;
import com.webflow.external.payment.PaymentApproveRequest;
import com.webflow.external.payment.PaymentApproveResponse;
import com.webflow.external.payment.PaymentClient;
import com.webflow.order.dao.OrderDao;
import com.webflow.order.domain.Order;
import com.webflow.order.dto.OrderResponse;
import com.webflow.order.service.OrderService;
import com.webflow.product.dao.ProductDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * [Web Step 3 — example B] 결제 비즈니스 규칙 — 외부 호출 "전후"의 방어선
 *
 * PaymentClient는 @Mock — 이 테스트의 관심은 HTTP가 아니라 규칙이다:
 *   ① 외부 호출 "전" 차단 (없는 주문 / 이미 결제됨 → PG 호출 자체가 없어야 한다)
 *   ② 외부 결과 "후" 처리 (승인 → PAID, 거절 → 주문 보존)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("주문 결제 서비스")
class OrderPaymentServiceTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private ProductDao productDao;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    private Order pendingOrder(Long orderId, long totalPrice) {
        return Order.builder()
                .orderId(orderId).productId(1L).quantity(1)
                .totalPrice(totalPrice).status(Order.STATUS_PENDING_PAYMENT)
                .build();
    }

    @Test
    @DisplayName("승인되면 PAID로 전이되고 paymentKey가 저장된다 — 금액은 주문 총액 그대로")
    void payOrder_결제대기주문_승인후PAID전이() {
        // given : findById는 두 번 불린다 — 검증 시점(PENDING), 응답 조회 시점(PAID)
        Order paid = Order.builder().orderId(1L).status(Order.STATUS_PAID)
                .paymentKey("PAY-2026-001").totalPrice(89000).build();
        given(orderDao.findById(1L)).willReturn(pendingOrder(1L, 89000), paid);
        given(paymentClient.approve(any(PaymentApproveRequest.class)))
                .willReturn(PaymentApproveResponse.builder()
                        .paymentKey("PAY-2026-001").status(PaymentApproveResponse.STATUS_APPROVED)
                        .build());

        // when
        OrderResponse response = orderService.payOrder(1L);

        // then : PG에 보낸 금액이 주문 총액인가 — Captor로 내용물 검증
        ArgumentCaptor<PaymentApproveRequest> captor =
                ArgumentCaptor.forClass(PaymentApproveRequest.class);
        then(paymentClient).should().approve(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(1L);
        assertThat(captor.getValue().getAmount()).isEqualTo(89000);

        then(orderDao).should().updateStatus(1L, Order.STATUS_PAID, "PAY-2026-001");
        assertThat(response.getStatus()).isEqualTo(Order.STATUS_PAID);
    }

    @Test
    @DisplayName("없는 주문이면 404 — PG 호출 자체가 없어야 한다")
    void payOrder_없는주문_404_PG호출차단() {
        // given
        given(orderDao.findById(99L)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> orderService.payOrder(99L))
                .isInstanceOf(OrderNotFoundException.class);

        // 외부 호출 "전" 방어선 — never가 핵심 검증이다 (과금 사고 방지!)
        then(paymentClient).should(never()).approve(any());
    }

    @Test
    @DisplayName("이미 PAID인 주문은 거부 — 이중 결제 방어도 PG 호출 전에")
    void payOrder_이미결제된주문_거부_PG호출차단() {
        // given
        Order alreadyPaid = Order.builder()
                .orderId(1L).status(Order.STATUS_PAID).totalPrice(89000).build();
        given(orderDao.findById(1L)).willReturn(alreadyPaid);

        // when & then : 같은 주문을 두 번 결제하면 고객 카드에서 두 번 빠진다!
        assertThatThrownBy(() -> orderService.payOrder(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("결제 대기 상태");

        then(paymentClient).should(never()).approve(any());
    }

    @Test
    @DisplayName("거절되면 예외 — 그리고 주문은 PENDING_PAYMENT 그대로 보존된다")
    void payOrder_거절_주문보존() {
        // given
        given(orderDao.findById(1L)).willReturn(pendingOrder(1L, 89000));
        given(paymentClient.approve(any(PaymentApproveRequest.class)))
                .willReturn(PaymentApproveResponse.builder()
                        .status(PaymentApproveResponse.STATUS_DECLINED).message("한도 초과")
                        .build());

        // when & then
        assertThatThrownBy(() -> orderService.payOrder(1L))
                .isInstanceOf(PaymentDeclinedException.class)
                .hasMessageContaining("한도 초과");

        // 주문을 건드리지 않았다 = 사용자가 다른 카드로 재시도할 수 있다
        then(orderDao).should(never()).updateStatus(anyLong(), anyString(), any());
    }
}
