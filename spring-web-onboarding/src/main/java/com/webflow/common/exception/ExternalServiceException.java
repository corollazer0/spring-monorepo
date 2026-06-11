package com.webflow.common.exception;

/**
 * 외부 서비스 장애 (타임아웃, 5xx, 연결 실패) — 503 계열.
 *
 * BusinessException을 상속하지 않는 이유: 이건 비즈니스 규칙 위반(사용자 잘못, 4xx)이
 * 아니라 인프라 사건이다. 사용자에게는 "잠시 후 재시도"(503)가 정직한 답이고,
 * 원인 예외(cause)는 로그로 남겨 우리가 추적한다.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String serviceName, Throwable cause) {
        super("외부 서비스(" + serviceName + ")가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요.", cause);
    }
}
