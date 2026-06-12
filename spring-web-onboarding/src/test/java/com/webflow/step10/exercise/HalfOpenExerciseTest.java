package com.webflow.step10.exercise;

import com.webflow.external.delivery.DeliveryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.test.web.client.MockRestServiceServer;

/**
 * [Web Step 10 — exercise] 반열림의 나머지 절반 — 탐사가 실패하면?
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (힌트: example의 회복 테스트와 대칭 — 응답만 5xx로)
 * 3. .\gradlew :spring-web-onboarding:test 로 통과를 확인한다
 *
 * example은 "반열림 → 성공 → 닫힘"을 봉인했다. 회복 시나리오의 나머지 절반,
 * "반열림 → 실패 → 다시 열림"이 없으면 상태 기계의 한쪽 문이 검증 밖이다.
 */
@Disabled("과제: docs/web/education/FOR-WebFlow-Step10.md 참고 후 @Disabled를 제거하고 완성하세요")
@RestClientTest(DeliveryClient.class)
@DisplayName("반열림 재실패 (연습문제)")
class HalfOpenExerciseTest {

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
        // given : ① 회로를 OPEN → HALF_OPEN으로 직접 전이하세요 (transitionTo...)
        //         ② 서버가 정확히 2번 5xx로 응답하도록 선언하세요 (탐사 허용 = 2회)
        // TODO 1
        // TODO 2

        // when & then : ③ track 호출이 ExternalServiceException으로 터지는지 검증하세요
        //               (탐사 2번 실패 → 회로 재OPEN → 3번째 재시도는 CallNotPermitted)
        // TODO 3

        // then : ④ 회로 상태가 OPEN인지 + server.verify()로 정확히 2번만 나갔는지 검증하세요
        // TODO 4
    }
}
