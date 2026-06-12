package com.webflow.external.delivery;

import com.webflow.common.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
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
 * 외부 배송사 조회 클라이언트 (Step 9 캡스톤 → Step 10 심화에서 서킷 브레이커 보강).
 *
 * PaymentClient와 같은 골격(빌더+rootUri+타임아웃+재시도+503 번역)에
 * 캡스톤만의 판단 하나가 더해진다:
 *   배송사 404 = "아직 송장 등록 전" — 장애도 오류도 아닌 정상 시나리오라서
 *   PREPARING(준비 중) 값으로 번역한다. (조회 GET이라 재시도 부담도 없다 — 멱등!)
 *
 * Step 10 — 서킷 브레이커:
 * 재시도(Step 4)는 "일시적" 장애 전제다. 외부가 완전히 죽은 동안엔 모든 요청이
 * 재시도+백오프를 풀로 겪고 실패한다 — 죽은 서버 폭격 + 응답 지연.
 * 차단기는 실패율이 임계치를 넘으면 회로를 열어(OPEN) 호출 자체를 즉시 거절하고,
 * 대기 후 반열림(HALF_OPEN)으로 살짝 열어 회복을 탐지한다.
 *
 * 호출 구조: retry( circuitBreaker( http ) ) — 시도 하나하나가 차단기에 기록되고,
 * 회로가 열리면 CallNotPermittedException은 재시도 대상이 아니라서 즉시 실패한다.
 */
@Slf4j
@Component
public class DeliveryClient {

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;
    private final CircuitBreaker circuitBreaker;

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

        // Step 10: 최근 10회 중 실패율 50% 이상이면 OPEN — 10초간 즉시 거절 후 반열림 탐지
        this.circuitBreaker = CircuitBreaker.of("delivery", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(10)              // 표본이 모이기 전엔 판단하지 않는다
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(2)
                // 장애(타임아웃·5xx)만 실패로 센다 — 404(송장 미등록)는 "응답을 준" 정상 호출
                .recordExceptions(ResourceAccessException.class, HttpServerErrorException.class)
                .build());
    }

    /**
     * 배송 상태 조회 — GET {base-url}/api/v1/deliveries/{paymentKey}
     * (배송사는 결제 키로 송장을 찾아준다는 계약)
     */
    public DeliveryStatusResponse track(String paymentKey) {
        log.info(">>>>> [DeliveryClient] 배송 조회. paymentKey={}", paymentKey);
        try {
            return retryTemplate.execute(context ->
                    circuitBreaker.executeSupplier(() ->
                            restTemplate.getForObject("/api/v1/deliveries/{paymentKey}",
                                    DeliveryStatusResponse.class, paymentKey)));
        } catch (HttpClientErrorException.NotFound e) {
            // 404 = 송장 미등록 — 실패가 아니라 "준비 중"이라는 비즈니스 값
            log.info(">>>>> [DeliveryClient] 송장 미등록 — 준비 중으로 간주. paymentKey={}", paymentKey);
            return DeliveryStatusResponse.preparing();
        } catch (CallNotPermittedException e) {
            // 회로 OPEN — 시도조차 안 했다 (죽은 서버를 두드리지 않고, 사용자를 기다리게 하지 않는다)
            log.warn(">>>>> [WARN] 배송사 회로 OPEN — 즉시 차단. state={}", circuitBreaker.getState());
            throw new ExternalServiceException("배송", e);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            throw new ExternalServiceException("배송", e);
        }
    }

    /** 상태 관찰/테스트용 — 차단기는 "상태를 가진 객체"라서 밖에서 들여다볼 창이 필요하다 */
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }
}
