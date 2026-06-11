package com.webflow.step03.answer;

import com.webflow.common.exception.OrderNotFoundException;
import com.webflow.common.exception.PaymentDeclinedException;
import com.webflow.order.controller.OrderController;
import com.webflow.order.domain.Order;
import com.webflow.order.dto.OrderResponse;
import com.webflow.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Web Step 3 — answer] PaymentApiExerciseTest 모범답안
 *
 * 채점 포인트: 성공 계약(200+필드) / 거절(400) / 없음(404) —
 * 예외 → HTTP 번역은 advice의 일이고, 이 슬라이스가 그 번역을 봉인한다.
 */
@WebMvcTest(OrderController.class)
@DisplayName("결제 API (모범답안)")
class PaymentApiAnswerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("결제 성공: POST /api/orders/1/payment → 200 + PAID 응답")
    void payOrder_성공_200() throws Exception {
        // given (TODO 1 답)
        given(orderService.payOrder(1L)).willReturn(OrderResponse.builder()
                .orderId(1L).productId(1L).quantity(1).totalPrice(89000)
                .status(Order.STATUS_PAID).paymentKey("PAY-2026-001")
                .build());

        // when & then (TODO 2 답)
        mockMvc.perform(post("/api/orders/1/payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentKey").value("PAY-2026-001"));
    }

    @Test
    @DisplayName("결제 거절: PaymentDeclinedException → 400 + 메시지")
    void payOrder_거절_400() throws Exception {
        // given (TODO 3 답)
        given(orderService.payOrder(1L))
                .willThrow(new PaymentDeclinedException(1L, "한도 초과"));

        // when & then (TODO 4 답)
        mockMvc.perform(post("/api/orders/1/payment"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("거절")));
    }

    @Test
    @DisplayName("없는 주문 결제: OrderNotFoundException → 404")
    void payOrder_없는주문_404() throws Exception {
        // given (TODO 5 답)
        given(orderService.payOrder(99L)).willThrow(new OrderNotFoundException(99L));

        // when & then (TODO 6 답)
        mockMvc.perform(post("/api/orders/99/payment"))
                .andExpect(status().isNotFound());
    }
}
