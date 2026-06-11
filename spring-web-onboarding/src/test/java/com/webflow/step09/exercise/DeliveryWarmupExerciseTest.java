package com.webflow.step09.exercise;

import com.webflow.external.delivery.DeliveryClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.test.web.client.MockRestServiceServer;

/**
 * [Web Step 9 — warmup exercise] 캡스톤 준비 운동 — 배송 클라이언트 첫 테스트
 *
 * 진행 방법:
 * 1. FOR-WebFlow-Step09.md의 요구사항·체크리스트를 먼저 읽는다 (스스로 설계!)
 * 2. 클래스 위의 @Disabled 를 지우고 TODO를 채운다
 * 3. 이 warmup이 돌면, answer 스위트를 "보기 전에" 나머지 시나리오를 직접 써본다
 *
 * 힌트: step03 PaymentClientTest와 같은 도구 — 단, 이번엔 GET이고
 * URL에 paymentKey가 들어간다 (/api/v1/deliveries/PAY-001).
 */
@Disabled("과제: docs/web/education/FOR-WebFlow-Step09.md 참고 후 @Disabled를 제거하고 완성하세요")
@RestClientTest(DeliveryClient.class)
@DisplayName("배송 클라이언트 (준비 운동)")
class DeliveryWarmupExerciseTest {

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    @DisplayName("배송 중 응답: GET 요청이 규격대로 나가고 SHIPPING이 매핑된다")
    void track_배송중응답_매핑() {
        // given : ① server.expect로 "/api/v1/deliveries/PAY-001"에 대한 GET 기대를 선언하고
        //         ② {"status": "SHIPPING", "invoiceNo": "INV-123", "courierName": "한진"}
        //            JSON을 withSuccess로 돌려주도록 하세요
        // TODO 1

        // when : ③ deliveryClient.track("PAY-001") 호출
        // TODO 2

        // then : ④ status가 SHIPPING, invoiceNo가 INV-123인지 + server.verify()
        // TODO 3
    }

    @Test
    @DisplayName("404 응답: 장애가 아니라 '준비 중'으로 번역된다 (캡스톤의 판단 포인트!)")
    void track_송장미등록404_준비중() {
        // given : ⑤ withStatus(HttpStatus.NOT_FOUND)로 404를 돌려주도록 하세요
        // TODO 4

        // when & then : ⑥ 예외가 아니라 PREPARING 상태가 반환되는지 검증하세요
        //               (외부의 404가 항상 우리의 404는 아니다!)
        // TODO 5
    }
}
