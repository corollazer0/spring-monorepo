package com.webflow.external.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 PG사 결제 클라이언트.
 *
 * RestTemplate을 직접 new 하지 않고 RestTemplateBuilder로 만드는 이유:
 * 1. @RestClientTest가 빌더에 MockRestServiceServer를 끼워 넣을 수 있다 (테스트 가능성!)
 * 2. rootUri로 베이스 URL을 한 곳에서 관리한다
 * (타임아웃이 아직 없다 — 이 "구멍"이 Step 4의 출발점이 된다)
 */
@Slf4j
@Component
public class PaymentClient {

    private final RestTemplate restTemplate;

    public PaymentClient(RestTemplateBuilder builder,
                         @Value("${external.payment.base-url}") String baseUrl) {
        this.restTemplate = builder
                .rootUri(baseUrl)
                .build();
    }

    /**
     * 결제 승인 요청 — POST {base-url}/api/v1/payments
     */
    public PaymentApproveResponse approve(PaymentApproveRequest request) {
        log.info(">>>>> [PaymentClient] 결제 승인 요청. orderId={}, amount={}",
                request.getOrderId(), request.getAmount());
        return restTemplate.postForObject("/api/v1/payments", request, PaymentApproveResponse.class);
    }
}
