package com.webflow.step04.exercise;

import com.webflow.external.payment.PaymentClient;
import com.webflow.order.dao.OrderDao;
import com.webflow.order.service.OrderService;
import com.webflow.product.dao.ProductDao;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * [Web Step 4 — exercise] 장애가 지나간 자리를 검증해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (힌트: step03의 OrderPaymentServiceTest — 거절 시나리오와 비교!)
 * 3. .\gradlew :spring-web-onboarding:test 로 통과를 확인한다
 *
 * 질문: 거절(DECLINED)과 장애(ExternalServiceException)의 테스트가 거의 같은 모양인데,
 * 왜 따로 봉인하나? — 사건이 다르면(400 vs 503) 회귀도 따로 일어나기 때문이다.
 */
@Disabled("과제: docs/web/education/FOR-WebFlow-Step04.md 참고 후 @Disabled를 제거하고 완성하세요")
@ExtendWith(MockitoExtension.class)
@DisplayName("결제 장애 시 주문 보존 (연습문제)")
class PaymentOutageExerciseTest {

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
        // given : ① findById가 PENDING_PAYMENT 주문을 반환하도록 스텁하세요
        //         ② paymentClient.approve가 ExternalServiceException을 던지도록 스텁하세요
        //            (new ExternalServiceException("결제", new RuntimeException("timeout")))
        // TODO 1
        // TODO 2

        // when & then : ③ payOrder가 ExternalServiceException으로 터지는지 검증하세요
        // TODO 3

        // then : ④ 가장 중요한 검증 — orderDao.updateStatus가 "한 번도" 불리지 않았는지
        //         (장애로 결제 여부가 불확실한데 PAID로 바꾸면? CANCELLED로 바꾸면? 둘 다 거짓말!)
        // TODO 4
    }
}
