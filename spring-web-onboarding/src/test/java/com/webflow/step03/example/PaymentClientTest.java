package com.webflow.step03.example;

import com.webflow.external.payment.PaymentApproveRequest;
import com.webflow.external.payment.PaymentApproveResponse;
import com.webflow.external.payment.PaymentClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * [Web Step 3 — example A] 외부 클라이언트 테스트 — @RestClientTest + MockRestServiceServer
 *
 * 진짜 PG를 호출하면: 과금되고, 느리고, PG 상태에 따라 결과가 달라진다(비결정).
 * MockRestServiceServer는 RestTemplate 바로 아래에서 HTTP를 가로채는 가짜 서버 —
 * 네트워크 없이 "요청이 규격대로 나갔는가"와 "응답을 규격대로 읽는가"를 검증한다.
 *
 * 검증 대상 = 우리 코드의 양 끝:
 *   나가는 요청(URL/메서드/본문)  +  들어오는 응답의 역직렬화
 */
@RestClientTest(PaymentClient.class)
@DisplayName("결제 클라이언트")
class PaymentClientTest {

    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    private MockRestServiceServer server;   // @RestClientTest가 빌더에 끼워 넣어준다

    @Test
    @DisplayName("승인: 요청 규격(URL/메서드/본문)대로 나가고, 응답이 객체로 매핑된다")
    void approve_승인응답_요청과매핑검증() {
        // given : 기대하는 "나가는 요청"을 먼저 선언하고, 돌려줄 응답을 준비
        // rootUri를 쓰면 기대 URL은 "상대 경로"로 — @RestClientTest가 루트를 떼고 매칭한다
        server.expect(requestTo("/api/v1/payments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.amount").value(89000))
                .andRespond(withSuccess(
                        "{\"paymentKey\": \"PAY-2026-001\", \"status\": \"APPROVED\", \"message\": null}",
                        MediaType.APPLICATION_JSON));

        // when
        PaymentApproveResponse response = paymentClient.approve(new PaymentApproveRequest(1L, 89000));

        // then : 역직렬화 검증
        assertThat(response.isApproved()).isTrue();
        assertThat(response.getPaymentKey()).isEqualTo("PAY-2026-001");

        // 선언한 기대가 전부 소비되었는지 — 호출 자체가 안 됐다면 여기서 잡힌다
        server.verify();
    }

    @Test
    @DisplayName("거절: HTTP 200 + DECLINED — 거절은 오류가 아니라 정상 응답이다")
    void approve_거절응답_정상매핑() {
        // given : PG는 멀쩡히 일했고, 결과가 거절일 뿐 (장애와 구별하라 — Step 4)
        server.expect(requestTo("/api/v1/payments"))
                .andRespond(withSuccess(
                        "{\"paymentKey\": null, \"status\": \"DECLINED\", \"message\": \"한도 초과\"}",
                        MediaType.APPLICATION_JSON));

        // when
        PaymentApproveResponse response = paymentClient.approve(new PaymentApproveRequest(2L, 999999));

        // then : 예외가 아니라 "거절"이라는 값 — 해석은 Service의 몫
        assertThat(response.isApproved()).isFalse();
        assertThat(response.getMessage()).isEqualTo("한도 초과");
        server.verify();
    }
}
