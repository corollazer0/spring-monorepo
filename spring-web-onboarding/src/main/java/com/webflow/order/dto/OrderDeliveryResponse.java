package com.webflow.order.dto;

import com.webflow.external.delivery.DeliveryStatusResponse;
import lombok.Builder;
import lombok.Getter;

/**
 * 주문 배송 조회 응답 (Step 9 캡스톤).
 *
 * 배송사 DTO(DeliveryStatusResponse)를 그대로 내보내지 않는 이유:
 * 우리 API의 계약이 배송사 API의 계약에 묶이면, 배송사 교체/변경이
 * 우리 클라이언트(앱·웹)의 변경으로 번진다 — 경계 DTO는 양방향이다.
 */
@Getter
@Builder
public class OrderDeliveryResponse {

    private final Long orderId;
    private final String deliveryStatus;   // PREPARING / SHIPPING / DELIVERED
    private final String invoiceNo;
    private final String courierName;

    public static OrderDeliveryResponse of(Long orderId, DeliveryStatusResponse delivery) {
        return OrderDeliveryResponse.builder()
                .orderId(orderId)
                .deliveryStatus(delivery.getStatus())
                .invoiceNo(delivery.getInvoiceNo())
                .courierName(delivery.getCourierName())
                .build();
    }
}
