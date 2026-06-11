package com.webflow.step04.answer;

import com.webflow.common.exception.ExternalServiceException;
import com.webflow.external.payment.PaymentClient;
import com.webflow.order.dao.OrderDao;
import com.webflow.order.domain.Order;
import com.webflow.order.service.OrderService;
import com.webflow.product.dao.ProductDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * [Web Step 4 — answer] PaymentOutageExerciseTest 모범답안
 *
 * 채점 포인트: 예외 전파 검증보다 TODO 4(never)가 본질이다 —
 * "결제 여부가 불확실할 때는 아무것도 단정하지 않는다"는 설계의 봉인.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("결제 장애 시 주문 보존 (모범답안)")
class PaymentOutageAnswerTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private ProductDao productDao;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("PG 장애 시 예외는 전파되고, 주문은 PENDING_PAYMENT 그대로 남는다")
    void payOrder_PG장애_주문보존() {
        // given (TODO 1, 2 답)
        given(orderDao.findById(1L)).willReturn(Order.builder()
                .orderId(1L).productId(1L).quantity(1)
                .totalPrice(89000).status(Order.STATUS_PENDING_PAYMENT)
                .build());
        given(paymentClient.approve(any()))
                .willThrow(new ExternalServiceException("결제", new RuntimeException("timeout")));

        // when & then (TODO 3 답)
        assertThatThrownBy(() -> orderService.payOrder(1L))
                .isInstanceOf(ExternalServiceException.class);

        // then (TODO 4 답) : 불확실한 상태에서는 어느 쪽으로도 단정하지 않는다
        then(orderDao).should(never()).updateStatus(anyLong(), anyString(), any());
    }
}
