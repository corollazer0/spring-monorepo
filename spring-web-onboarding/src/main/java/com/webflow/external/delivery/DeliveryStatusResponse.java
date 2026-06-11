package com.webflow.external.delivery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 배송사 조회 응답 — 외부 계약 전용 DTO (Step 9 캡스톤).
 *
 * status: PREPARING(준비 중) / SHIPPING(배송 중) / DELIVERED(완료)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatusResponse {

    public static final String STATUS_PREPARING = "PREPARING";
    public static final String STATUS_SHIPPING = "SHIPPING";
    public static final String STATUS_DELIVERED = "DELIVERED";

    private String status;
    private String invoiceNo;
    private String courierName;

    /** 배송사에 아직 정보가 없을 때(404)의 기본 상태 — "준비 중"으로 간주 */
    public static DeliveryStatusResponse preparing() {
        return DeliveryStatusResponse.builder()
                .status(STATUS_PREPARING)
                .build();
    }
}
