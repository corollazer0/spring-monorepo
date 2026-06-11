package com.webflow.order.dto;

import com.webflow.order.domain.Order;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 주문 응답 DTO.
 */
@Getter
@Builder
public class OrderResponse {

    private final Long orderId;
    private final Long productId;
    private final int quantity;
    private final long totalPrice;
    private final String status;
    private final String paymentKey;
    private final LocalDateTime orderedAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .paymentKey(order.getPaymentKey())
                .orderedAt(order.getOrderedAt())
                .build();
    }
}
