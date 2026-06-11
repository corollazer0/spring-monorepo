package com.webflow.step09.answer;

import com.webflow.common.exception.BusinessException;
import com.webflow.common.exception.ExternalServiceException;
import com.webflow.order.controller.DeliveryController;
import com.webflow.order.dto.OrderDeliveryResponse;
import com.webflow.order.service.DeliveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Web Step 9 — answer C] 배송 조회 API의 HTTP 계약 — 200/400/503 번역
 */
@WebMvcTest(DeliveryController.class)
@DisplayName("배송 조회 API (모범답안)")
class DeliveryApiAnswerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeliveryService deliveryService;

    @Test
    @DisplayName("성공: GET /api/orders/1/delivery → 200 + 배송 정보")
    void getDelivery_성공_200() throws Exception {
        // given
        given(deliveryService.getDeliveryStatus(1L)).willReturn(OrderDeliveryResponse.builder()
                .orderId(1L).deliveryStatus("SHIPPING")
                .invoiceNo("INV-123").courierName("한진")
                .build());

        // when & then
        mockMvc.perform(get("/api/orders/1/delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.deliveryStatus").value("SHIPPING"))
                .andExpect(jsonPath("$.invoiceNo").value("INV-123"));
    }

    @Test
    @DisplayName("미결제 주문: 400 + 안내 메시지")
    void getDelivery_미결제_400() throws Exception {
        // given
        given(deliveryService.getDeliveryStatus(3L))
                .willThrow(new BusinessException("결제 완료된 주문만 배송 조회가 가능합니다. 현재 상태: PENDING_PAYMENT"));

        // when & then
        mockMvc.perform(get("/api/orders/3/delivery"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("결제 완료된 주문만")));
    }

    @Test
    @DisplayName("배송사 장애: 503 — Step 4의 번역 규약이 새 기능에도 그대로 적용된다")
    void getDelivery_배송사장애_503() throws Exception {
        // given
        given(deliveryService.getDeliveryStatus(1L)).willThrow(
                new ExternalServiceException("배송", new ResourceAccessException("Read timed out")));

        // when & then
        mockMvc.perform(get("/api/orders/1/delivery"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(containsString("잠시 후 다시 시도")));
    }
}
