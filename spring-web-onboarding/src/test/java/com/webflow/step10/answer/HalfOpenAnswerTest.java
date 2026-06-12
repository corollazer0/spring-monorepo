package com.webflow.step10.answer;

import com.webflow.common.exception.ExternalServiceException;
import com.webflow.external.delivery.DeliveryClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

/**
 * [Web Step 10 — answer] HalfOpenExerciseTest 모범답안
 *
 * 채점 포인트: server.verify()로 "정확히 2번"을 봉인했는가 —
 * 반열림의 탐사 횟수 제한(permittedNumberOfCalls=2)이 진짜 동작한다는 증거다.
 * (제한이 없다면 회복 탐사가 또 다른 폭격이 된다!)
 */
@RestClientTest(DeliveryClient.class)
@DisplayName("반열림 재실패 (모범답안)")
class HalfOpenAnswerTest {

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
    @DisplayName("반열림에서 탐사 2번이 또 실패하면 — 회로는 다시 OPEN으로 돌아간다")
    void track_반열림_재실패시다시열림() {
        // given (TODO 1, 2 답)
        deliveryClient.getCircuitBreaker().transitionToOpenState();
        deliveryClient.getCircuitBreaker().transitionToHalfOpenState();
        server.expect(times(2), requestTo(URL)).andRespond(withServerError());

        // when & then (TODO 3 답) : 탐사 1·2회차 실패 → 재OPEN → 3회차는 차단
        assertThatThrownBy(() -> deliveryClient.track("PAY-001"))
                .isInstanceOf(ExternalServiceException.class);

        // then (TODO 4 답) : 다시 닫힌 문 + 탐사는 허용된 2번뿐
        assertThat(deliveryClient.getCircuitBreaker().getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
        server.verify();
    }
}
