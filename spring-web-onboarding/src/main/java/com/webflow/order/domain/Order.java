package com.webflow.order.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 주문 도메인.
 *
 * 상태 흐름: PENDING_PAYMENT(생성) → PAID(결제 승인, Step 3) / CANCELLED(거절·정리)
 * (테이블명은 orders — ORDER는 SQL 예약어!)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private Long orderId;
    private Long productId;
    private int quantity;
    private long totalPrice;
    private String status;
    private String paymentKey; // 결제 승인 키 (Step 3)
    private LocalDateTime orderedAt;
}
