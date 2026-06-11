package com.webflow.step03.exercise;

import com.webflow.order.controller.OrderController;
import com.webflow.order.service.OrderService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * [Web Step 3 — exercise] 결제 API의 HTTP 계약을 검증해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (힌트: step02의 ProductListApiTest, step01의 예외 매핑 패턴)
 * 3. .\gradlew :spring-web-onboarding:test 로 통과를 확인한다
 *
 * 포인트: 여기는 @WebMvcTest — PaymentClient는 등장하지 않는다!
 * Service를 @MockBean으로 막았으니 외부 연동은 이 슬라이스 밖의 일이다.
 * (계층마다 자기 책임만 — Client는 @RestClientTest, 규칙은 Mockito, HTTP는 여기)
 */
@Disabled("과제: docs/web/education/FOR-WebFlow-Step03.md 참고 후 @Disabled를 제거하고 완성하세요")
@WebMvcTest(OrderController.class)
@DisplayName("결제 API (연습문제)")
class PaymentApiExerciseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("결제 성공: POST /api/orders/1/payment → 200 + PAID 응답")
    void payOrder_성공_200() throws Exception {
        // given : orderService.payOrder(1L)이 PAID 상태의 OrderResponse를 반환하도록 스텁하세요
        //         (OrderResponse.builder()... — paymentKey도 채워서)
        // TODO 1

        // when & then : POST 후 status 200, jsonPath로 $.status == PAID, $.paymentKey 검증
        // TODO 2
    }

    @Test
    @DisplayName("결제 거절: PaymentDeclinedException → 400 + 메시지")
    void payOrder_거절_400() throws Exception {
        // given : payOrder가 PaymentDeclinedException을 던지도록 스텁하세요
        //         (new PaymentDeclinedException(1L, "한도 초과"))
        // TODO 3

        // when & then : 400 + $.message에 "거절" 포함 검증
        // TODO 4
    }

    @Test
    @DisplayName("없는 주문 결제: OrderNotFoundException → 404")
    void payOrder_없는주문_404() throws Exception {
        // given : payOrder(99L)가 OrderNotFoundException을 던지도록 스텁하세요
        // TODO 5

        // when & then : 404 검증
        // TODO 6
    }
}
