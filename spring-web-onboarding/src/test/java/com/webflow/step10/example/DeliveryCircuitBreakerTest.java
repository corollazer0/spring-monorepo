package com.webflow.step10.example;

import com.webflow.common.exception.ExternalServiceException;
import com.webflow.external.delivery.DeliveryClient;
import com.webflow.external.delivery.DeliveryStatusResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * [Web Step 10 — example] 서킷 브레이커의 3상 — CLOSED / OPEN / HALF_OPEN
 *
 * 차단기는 "상태를 가진 객체"다 — 그래서 두 가지가 따라온다:
 * 1. 테스트마다 reset() — 앞 테스트가 열어둔 회로가 뒷 테스트를 오염시킨다
 *    (Step 6 캐시 clear와 같은 철학: 살아남는 상태는 직접 격리!)
 * 2. 시간 의존(OPEN 10초 대기)은 기다리지 않는다 — transitionTo...() 로
 *    상태를 직접 주입한다 (Step 7의 시각 주입과 같은 철학)
 */
@RestClientTest(DeliveryClient.class)
@DisplayName("배송 클라이언트 서킷 브레이커")
class DeliveryCircuitBreakerTest {

    private static final String URL = "/api/v1/deliveries/PAY-001";

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private MockRestServiceServer server;

    @BeforeEach
    void resetCircuit() {
        deliveryClient.getCircuitBreaker().reset();
    }

    @Test
    @DisplayName("CLOSED: 평소엔 차단기가 있는지도 모른다 — 호출은 그냥 통과한다")
    void track_정상_회로닫힘유지() {
        // given
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"status\": \"SHIPPING\", \"invoiceNo\": \"INV-123\", \"courierName\": \"한진\"}",
                MediaType.APPLICATION_JSON));

        // when
        DeliveryStatusResponse response = deliveryClient.track("PAY-001");

        // then
        assertThat(response.getStatus()).isEqualTo(DeliveryStatusResponse.STATUS_SHIPPING);
        assertThat(deliveryClient.getCircuitBreaker().getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("OPEN 전이: 실패가 쌓이면(10회 중 50%↑) 회로가 열리고, 11번째 폭격은 없다")
    void track_연속장애_회로열림() {
        // given : 배송사가 완전히 죽었다 — 정확히 10번만 응답하도록 선언
        //         (11번째 요청이 나가면 mock 서버가 그 자리에서 실패시킨다 = 차단의 증명)
        server.expect(times(10), requestTo(URL)).andRespond(withServerError());

        // when : track 4번 — 3번은 재시도 3회씩(9 호출), 4번째의 1회가 10번째 호출
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> deliveryClient.track("PAY-001"))
                    .isInstanceOf(ExternalServiceException.class);
        }
        // 4번째: 첫 시도(10번째 호출)가 기록되는 순간 회로가 열린다 →
        //        재시도 2회차는 HTTP 없이 CallNotPermitted로 즉시 실패
        assertThatThrownBy(() -> deliveryClient.track("PAY-001"))
                .isInstanceOf(ExternalServiceException.class)
                .hasCauseInstanceOf(CallNotPermittedException.class);

        // then : 회로 OPEN + 선언한 10번에서 정확히 멈췄다
        assertThat(deliveryClient.getCircuitBreaker().getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
        server.verify();
    }

    @Test
    @DisplayName("OPEN: 호출 자체가 안 나간다 — HTTP 0회, 즉시 503 (기다림도 폭격도 없다)")
    void track_회로열림_즉시차단() {
        // given : 회로를 직접 OPEN으로 — 10초를 기다리지 않는 상태 주입
        //         server에는 기대 선언이 0개! 요청이 하나라도 나가면 그 자리에서 실패한다
        deliveryClient.getCircuitBreaker().transitionToOpenState();

        // when & then
        assertThatThrownBy(() -> deliveryClient.track("PAY-001"))
                .isInstanceOf(ExternalServiceException.class)
                .hasCauseInstanceOf(CallNotPermittedException.class);

        server.verify();   // 진짜 호출 0회 — Step 4의 재시도와 결정적으로 다른 점!
    }

    @Test
    @DisplayName("HALF_OPEN → CLOSED: 탐사 호출 2번이 성공하면 회로가 다시 닫힌다 (자동 회복)")
    void track_반열림_성공시회복() {
        // given : OPEN → (10초 대기 대신) HALF_OPEN 직접 전이 — 탐사 2회 허용 상태
        deliveryClient.getCircuitBreaker().transitionToOpenState();
        deliveryClient.getCircuitBreaker().transitionToHalfOpenState();
        server.expect(times(2), requestTo(URL)).andRespond(withSuccess(
                "{\"status\": \"DELIVERED\", \"invoiceNo\": \"INV-123\", \"courierName\": \"한진\"}",
                MediaType.APPLICATION_JSON));

        // when : 탐사 호출 2번 성공
        deliveryClient.track("PAY-001");
        deliveryClient.track("PAY-001");

        // then : 사람 개입 없이 스스로 닫혔다 — 차단기의 진짜 가치는 "자동 회복"
        assertThat(deliveryClient.getCircuitBreaker().getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
        server.verify();
    }
}
