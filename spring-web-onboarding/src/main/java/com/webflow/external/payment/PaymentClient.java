package com.webflow.external.payment;

import com.webflow.common.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;

/**
 * 외부 PG사 결제 클라이언트.
 *
 * RestTemplate을 직접 new 하지 않고 RestTemplateBuilder로 만드는 이유:
 * 1. @RestClientTest가 빌더에 MockRestServiceServer를 끼워 넣을 수 있다 (테스트 가능성!)
 * 2. rootUri로 베이스 URL을 한 곳에서 관리한다
 *
 * Step 4 — 장애 생존 3종 세트:
 * - 타임아웃: PG가 멈춰도 우리 스레드는 제한 시간만 기다린다 (스레드 풀 고갈 방지)
 * - 재시도: 일시적 장애(타임아웃/5xx)는 백오프를 두고 최대 3회 시도
 *   (결제 승인은 orderId가 멱등키 — PG가 같은 주문의 중복 승인을 막아준다는 계약 전제)
 * - 격리: 그래도 실패하면 ExternalServiceException(503)으로 번역 — 거절(400)과 다른 사건!
 */
@Slf4j
@Component
public class PaymentClient {

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    public PaymentClient(RestTemplateBuilder builder,
                         @Value("${external.payment.base-url}") String baseUrl,
                         @Value("${external.payment.connect-timeout-millis}") int connectTimeoutMillis,
                         @Value("${external.payment.read-timeout-millis}") int readTimeoutMillis) {
        this.restTemplate = builder
                .rootUri(baseUrl)
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMillis))
                .setReadTimeout(Duration.ofMillis(readTimeoutMillis))
                .build();

        // 백오프 없는 재시도는 금지 — 죽어가는 서버를 더 두드리는 짓이다
        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(100, 2.0, 1000)   // 100ms → 200ms (최대 1s)
                .retryOn(Arrays.asList(ResourceAccessException.class, HttpServerErrorException.class))
                .build();
    }

    /**
     * 결제 승인 요청 — POST {base-url}/api/v1/payments
     *
     * 재시도 대상은 "일시적" 장애만:
     * - ResourceAccessException(타임아웃/연결 실패), HttpServerErrorException(5xx) → 재시도
     * - 4xx(우리 요청이 잘못됨)는 재시도해도 똑같이 실패 — 그대로 터뜨려 버그로 드러낸다
     */
    public PaymentApproveResponse approve(PaymentApproveRequest request) {
        log.info(">>>>> [PaymentClient] 결제 승인 요청. orderId={}, amount={}",
                request.getOrderId(), request.getAmount());
        try {
            return retryTemplate.execute(context -> {
                if (context.getRetryCount() > 0) {
                    log.warn(">>>>> [WARN] 결제 승인 재시도 {}회차. orderId={}",
                            context.getRetryCount(), request.getOrderId());
                }
                return restTemplate.postForObject("/api/v1/payments", request, PaymentApproveResponse.class);
            });
        } catch (ResourceAccessException | HttpServerErrorException e) {
            // 재시도 끝에 포기 — 장애를 503 사건으로 번역 (원인은 cause로 보존)
            throw new ExternalServiceException("결제", e);
        }
    }
}
