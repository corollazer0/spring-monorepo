package com.webflow.step04.example;

import com.webflow.common.exception.ExternalServiceException;
import com.webflow.order.controller.OrderController;
import com.webflow.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Web Step 4 — example B] 장애의 HTTP 번역 — 503 Service Unavailable
 *
 * 상태코드는 책임의 표명이다:
 *   400 = 당신(요청) 문제  /  500 = 우리(버그) 문제  /  503 = "일시적입니다, 재시도하세요"
 * 장애를 500으로 내보내면 클라이언트는 재시도해야 할지 알 수 없다.
 */
@WebMvcTest(OrderController.class)
@DisplayName("결제 장애 API 번역")
class PaymentOutageApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("외부 장애: ExternalServiceException → 503 + 재시도 안내 메시지")
    void payOrder_외부장애_503() throws Exception {
        // given
        given(orderService.payOrder(1L)).willThrow(
                new ExternalServiceException("결제", new ResourceAccessException("Read timed out")));

        // when & then : 내부 사정(타임아웃, URL 등)은 숨기고 행동 지침만 준다
        mockMvc.perform(post("/api/orders/1/payment"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(containsString("잠시 후 다시 시도")));
    }
}
