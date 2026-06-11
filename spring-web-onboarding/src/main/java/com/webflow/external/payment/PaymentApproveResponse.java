package com.webflow.external.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PG사 결제 승인 응답.
 *
 * status: APPROVED(승인) / DECLINED(거절 — 한도 초과, 분실 카드 등)
 * 거절은 "오류"가 아니라 정상 응답(HTTP 200)이다 — PG가 일을 잘 한 결과!
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentApproveResponse {

    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_DECLINED = "DECLINED";

    private String paymentKey;
    private String status;
    private String message;

    public boolean isApproved() {
        return STATUS_APPROVED.equals(status);
    }
}
