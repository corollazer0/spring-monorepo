package com.webflow.step04.example;

import com.webflow.common.exception.ExternalServiceException;
import com.webflow.external.payment.PaymentApproveRequest;
import com.webflow.external.payment.PaymentApproveResponse;
import com.webflow.external.payment.PaymentClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * [Web Step 4 — example A] 재시도의 세 얼굴 — 회복 / 포기 / 비재시도
 *
 * 재시도 "횟수"를 어떻게 검증하나? MockRestServiceServer의 기대 선언이 곧 검증이다:
 * - 순차 expect 3개 → 정확히 3번, 선언한 순서의 응답 (실패→실패→성공 시나리오)
 * - ExpectedCount.times(3) → 같은 응답 3번 (전부 실패 시나리오)
 * server.verify()가 "선언한 만큼만 소비됐는가"를 봉인한다 — 무한 재시도도, 0회도 잡힌다.
 */
@RestClientTest(PaymentClient.class)
@DisplayName("결제 클라이언트 재시도")
class PaymentClientRetryTest {

    private static final String URL = "/api/v1/payments";   // rootUri 사용 → 상대 경로 매칭

    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    @DisplayName("일시 장애(5xx 2번) 후 성공 — 사용자는 장애가 있었는지도 모른다")
    void approve_일시장애후성공_회복() {
        // given : 순차 기대 — 1·2번째는 500, 3번째는 정상 응답
        server.expect(requestTo(URL)).andRespond(withServerError());
        server.expect(requestTo(URL)).andRespond(withServerError());
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"paymentKey\": \"PAY-2026-001\", \"status\": \"APPROVED\", \"message\": null}",
                MediaType.APPLICATION_JSON));

        // when : 예외 없이 그냥 성공한다 — 재시도는 호출자에게 투명하다
        PaymentApproveResponse response = paymentClient.approve(new PaymentApproveRequest(1L, 89000));

        // then
        assertThat(response.isApproved()).isTrue();
        server.verify();   // 정확히 3번 호출됐다 = 재시도 2회가 실제로 일어났다
    }

    @Test
    @DisplayName("연속 장애(5xx 3번) — maxAttempts에서 멈추고 503 사건으로 번역된다")
    void approve_연속장애_3회후포기() {
        // given : 같은 기대 3번 — 4번째 호출이 오면 그 자체로 실패한다 (무한 재시도 방지 검증!)
        server.expect(times(3), requestTo(URL)).andRespond(withServerError());

        // when & then : 원인(cause)이 보존되어 로그 추적이 가능하다
        assertThatThrownBy(() -> paymentClient.approve(new PaymentApproveRequest(1L, 89000)))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("잠시 후 다시 시도")
                .hasCauseInstanceOf(HttpServerErrorException.class);

        server.verify();   // 3번에서 정확히 멈췄다
    }

    @Test
    @DisplayName("타임아웃(IOException)도 재시도 대상 — 3회 후 503 사건")
    void approve_타임아웃_재시도후포기() {
        // given : 읽기 타임아웃 시뮬레이션 — RestTemplate이 ResourceAccessException으로 감싼다
        server.expect(times(3), requestTo(URL))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        // when & then
        assertThatThrownBy(() -> paymentClient.approve(new PaymentApproveRequest(1L, 89000)))
                .isInstanceOf(ExternalServiceException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);

        server.verify();
    }

    @Test
    @DisplayName("4xx는 재시도하지 않는다 — 우리 요청이 잘못된 건 다시 보내도 잘못이다")
    void approve_4xx_재시도없이즉시실패() {
        // given : 기대는 딱 1번 — 재시도가 일어나면 server.verify가 아니라 2번째 호출에서 깨진다
        server.expect(times(1), requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        // when & then : 503 번역 대상도 아니다 — 이건 장애가 아니라 우리 버그(500으로 드러나야 한다)
        assertThatThrownBy(() -> paymentClient.approve(new PaymentApproveRequest(1L, 89000)))
                .isInstanceOf(HttpClientErrorException.class);

        server.verify();   // 정확히 1번 — 의미 없는 재시도가 없었다
    }
}
