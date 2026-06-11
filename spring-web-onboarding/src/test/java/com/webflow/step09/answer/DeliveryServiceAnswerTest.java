package com.webflow.step09.answer;

import com.webflow.common.exception.BusinessException;
import com.webflow.common.exception.OrderNotFoundException;
import com.webflow.external.delivery.DeliveryClient;
import com.webflow.external.delivery.DeliveryStatusResponse;
import com.webflow.order.dao.OrderDao;
import com.webflow.order.domain.Order;
import com.webflow.order.dto.OrderDeliveryResponse;
import com.webflow.order.service.DeliveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * [Web Step 9 — answer B] DeliveryService 모범 스위트 — 외부 호출 전 방어선
 *
 * 채점 포인트: payOrder(Step 3)와 같은 구조를 스스로 재현했는가 —
 * 404/400 거부 시 배송사 호출이 "없어야"(never) 한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("배송 조회 서비스 (모범답안)")
class DeliveryServiceAnswerTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private DeliveryClient deliveryClient;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    @DisplayName("PAID 주문: paymentKey로 배송사를 조회하고 우리 DTO로 변환한다")
    void getDeliveryStatus_결제완료주문_조회성공() {
        // given
        given(orderDao.findById(1L)).willReturn(Order.builder()
                .orderId(1L).status(Order.STATUS_PAID).paymentKey("PAY-001").build());
        given(deliveryClient.track("PAY-001")).willReturn(DeliveryStatusResponse.builder()
                .status(DeliveryStatusResponse.STATUS_SHIPPING)
                .invoiceNo("INV-123").courierName("한진")
                .build());

        // when
        OrderDeliveryResponse response = deliveryService.getDeliveryStatus(1L);

        // then : 외부 DTO가 아니라 "우리 응답"의 모양으로
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getDeliveryStatus()).isEqualTo("SHIPPING");
        assertThat(response.getInvoiceNo()).isEqualTo("INV-123");
        then(deliveryClient).should().track("PAY-001");   // 연결 고리는 paymentKey!
    }

    @Test
    @DisplayName("없는 주문: 404 — 배송사 호출 없이")
    void getDeliveryStatus_없는주문_404_호출차단() {
        // given
        given(orderDao.findById(99L)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> deliveryService.getDeliveryStatus(99L))
                .isInstanceOf(OrderNotFoundException.class);
        then(deliveryClient).should(never()).track(anyString());
    }

    @Test
    @DisplayName("미결제 주문: 400 — 결제 안 된 주문의 배송이란 존재하지 않는다")
    void getDeliveryStatus_미결제주문_400_호출차단() {
        // given
        given(orderDao.findById(3L)).willReturn(Order.builder()
                .orderId(3L).status(Order.STATUS_PENDING_PAYMENT).build());

        // when & then
        assertThatThrownBy(() -> deliveryService.getDeliveryStatus(3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("결제 완료된 주문만");
        then(deliveryClient).should(never()).track(anyString());
    }

    @Test
    @DisplayName("취소된 주문도 400 — PAID '만' 허용 (화이트리스트 사고방식)")
    void getDeliveryStatus_취소주문_400() {
        // given
        given(orderDao.findById(6L)).willReturn(Order.builder()
                .orderId(6L).status(Order.STATUS_CANCELLED).build());

        // when & then : "PENDING이 아니면"이 아니라 "PAID가 아니면" 거부 —
        //               새 상태가 추가돼도 안전한 쪽으로 닫힌다
        assertThatThrownBy(() -> deliveryService.getDeliveryStatus(6L))
                .isInstanceOf(BusinessException.class);
        then(deliveryClient).should(never()).track(anyString());
    }
}
