package com.webflow.step09.answer;

import com.webflow.common.exception.ExternalServiceException;
import com.webflow.external.delivery.DeliveryClient;
import com.webflow.external.delivery.DeliveryStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * [Web Step 9 — answer A] DeliveryClient 모범 스위트 — 외부의 응답 4상
 *
 * 채점 포인트: 404를 어떻게 다뤘는가 — 배송사의 404는 "송장 미등록"이라는
 * 정상 시나리오다. 외부의 상태코드를 우리 의미로 "번역"하는 것이 클라이언트의 일.
 */
@RestClientTest(DeliveryClient.class)
@DisplayName("배송 클라이언트 (모범답안)")
class DeliveryClientAnswerTest {

    private static final String URL = "/api/v1/deliveries/PAY-001";

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    @DisplayName("배송 중: GET 규격 + SHIPPING 매핑 (warmup TODO 1~3 답)")
    void track_배송중응답_매핑() {
        // given
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"status\": \"SHIPPING\", \"invoiceNo\": \"INV-123\", \"courierName\": \"한진\"}",
                        MediaType.APPLICATION_JSON));

        // when
        DeliveryStatusResponse response = deliveryClient.track("PAY-001");

        // then
        assertThat(response.getStatus()).isEqualTo(DeliveryStatusResponse.STATUS_SHIPPING);
        assertThat(response.getInvoiceNo()).isEqualTo("INV-123");
        server.verify();
    }

    @Test
    @DisplayName("404: 장애가 아니라 '준비 중' — 외부 상태코드의 비즈니스 번역 (warmup TODO 4~5 답)")
    void track_송장미등록404_준비중() {
        // given
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        // when : 예외가 아니다!
        DeliveryStatusResponse response = deliveryClient.track("PAY-001");

        // then
        assertThat(response.getStatus()).isEqualTo(DeliveryStatusResponse.STATUS_PREPARING);
        server.verify();
    }

    @Test
    @DisplayName("5xx 연속: 3회 재시도 후 503 사건 (Step 4 패턴의 재적용)")
    void track_연속장애_재시도후포기() {
        // given
        server.expect(times(3), requestTo(URL)).andRespond(withServerError());

        // when & then
        assertThatThrownBy(() -> deliveryClient.track("PAY-001"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("배송");

        server.verify();
    }

    @Test
    @DisplayName("일시 장애 후 회복: 5xx 1번 뒤 성공 — 사용자는 모른다")
    void track_일시장애후성공_회복() {
        // given
        server.expect(requestTo(URL)).andRespond(withServerError());
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"status\": \"DELIVERED\", \"invoiceNo\": \"INV-123\", \"courierName\": \"한진\"}",
                MediaType.APPLICATION_JSON));

        // when
        DeliveryStatusResponse response = deliveryClient.track("PAY-001");

        // then
        assertThat(response.getStatus()).isEqualTo(DeliveryStatusResponse.STATUS_DELIVERED);
        server.verify();
    }
}
