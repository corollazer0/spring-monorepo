package com.webflow.common.exception;

/**
 * 존재하지 않는 주문 접근 시 발생. (404로 변환)
 */
public class OrderNotFoundException extends BusinessException {

    public OrderNotFoundException(Long orderId) {
        super("주문을 찾을 수 없습니다. orderId=" + orderId);
    }
}
