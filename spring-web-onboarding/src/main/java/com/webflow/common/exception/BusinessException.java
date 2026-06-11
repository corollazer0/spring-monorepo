package com.webflow.common.exception;

/**
 * 비즈니스 규칙 위반의 최상위 예외 (TestCraft와 동일 패턴).
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
