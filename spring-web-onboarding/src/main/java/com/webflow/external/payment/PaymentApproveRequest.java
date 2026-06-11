package com.webflow.external.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PG사 결제 승인 요청 — 외부 API의 계약 (우리 도메인과 분리!)
 *
 * Order 도메인을 그대로 보내지 않는 이유: 외부 계약이 바뀌어도(필드 추가/이름 변경)
 * 내부 도메인이 흔들리지 않아야 한다 — 경계에는 전용 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentApproveRequest {

    private Long orderId;
    private long amount;
}
