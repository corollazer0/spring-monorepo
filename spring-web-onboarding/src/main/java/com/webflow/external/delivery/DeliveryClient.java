package com.webflow.external.delivery;

import com.webflow.common.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;

/**
 * 외부 배송사 조회 클라이언트 (Step 9 캡스톤) — Step 3~4의 무기 종합.
 *
 * PaymentClient와 같은 골격(빌더+rootUri+타임아웃+재시도+503 번역)에
 * 캡스톤만의 판단 하나가 더해진다:
 *   배송사 404 = "아직 송장 등록 전" — 장애도 오류도 아닌 정상 시나리오라서
 *   PREPARING(준비 중) 값으로 번역한다. (조회 GET이라 재시도 부담도 없다 — 멱등!)
 */
@Slf4j
@Component
public class DeliveryClient {

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    public DeliveryClient(RestTemplateBuilder builder,
                          @Value("${external.delivery.base-url}") String baseUrl,
                          @Value("${external.delivery.connect-timeout-millis}") int connectTimeoutMillis,
                          @Value("${external.delivery.read-timeout-millis}") int readTimeoutMillis) {
        this.restTemplate = builder
                .rootUri(baseUrl)
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMillis))
                .setReadTimeout(Duration.ofMillis(readTimeoutMillis))
                .build();
        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(100, 2.0, 1000)
                .retryOn(Arrays.asList(ResourceAccessException.class, HttpServerErrorException.class))
                .build();
    }

    /**
     * 배송 상태 조회 — GET {base-url}/api/v1/deliveries/{paymentKey}
     * (배송사는 결제 키로 송장을 찾아준다는 계약)
     */
    public DeliveryStatusResponse track(String paymentKey) {
        log.info(">>>>> [DeliveryClient] 배송 조회. paymentKey={}", paymentKey);
        try {
            return retryTemplate.execute(context ->
                    restTemplate.getForObject("/api/v1/deliveries/{paymentKey}",
                            DeliveryStatusResponse.class, paymentKey));
        } catch (HttpClientErrorException.NotFound e) {
            // 404 = 송장 미등록 — 실패가 아니라 "준비 중"이라는 비즈니스 값
            log.info(">>>>> [DeliveryClient] 송장 미등록 — 준비 중으로 간주. paymentKey={}", paymentKey);
            return DeliveryStatusResponse.preparing();
        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new ExternalServiceException("배송", e);
        }
    }
}
