package com.webflow.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 주문 생성 요청 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderCreateRequest {

    @NotNull(message = "상품 ID는 필수입니다")
    private Long productId;

    @Min(value = 1, message = "수량은 1 이상이어야 합니다")
    private int quantity;

    public OrderCreateRequest(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
}
