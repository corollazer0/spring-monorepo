package com.webflow.common.exception;

/**
 * 존재하지 않는 상품 접근 시 발생. (404로 변환)
 */
public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(Long productId) {
        super("상품을 찾을 수 없습니다. productId=" + productId);
    }
}
