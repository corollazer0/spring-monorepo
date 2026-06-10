package com.testonboarding.common.exception;

/**
 * 비즈니스 규칙 위반을 나타내는 최상위 예외.
 *
 * 시스템 오류(NullPointerException 등)와 구분하기 위해 별도 계층을 둔다.
 * Step 5에서 GlobalExceptionHandler가 이 계층을 HTTP 상태코드로 변환한다.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
