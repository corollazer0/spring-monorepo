package com.webflow.common.exception;

/**
 * 재고 부족으로 주문할 수 없을 때 발생. (409 Conflict로 변환 —
 * 요청 형식은 맞지만 현재 자원 상태와 충돌한다는 의미)
 */
public class OutOfStockException extends BusinessException {

    public OutOfStockException(Long productId, int requested) {
        super("재고가 부족합니다. productId=" + productId + ", 요청 수량=" + requested);
    }
}
