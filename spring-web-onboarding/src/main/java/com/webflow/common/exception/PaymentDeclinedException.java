package com.webflow.common.exception;

/**
 * 결제 거절 (PG사가 정상 응답으로 DECLINED를 준 경우) — 400 계열.
 *
 * 주의: 거절(이 예외)과 장애(PG 무응답/5xx — Step 4)는 다른 사건이다.
 * 거절 = 사용자 카드 문제(400), 장애 = 우리·PG 사이 문제(503).
 */
public class PaymentDeclinedException extends BusinessException {

    public PaymentDeclinedException(Long orderId, String reason) {
        super("결제가 거절되었습니다. 주문번호: " + orderId + ", 사유: " + reason);
    }
}
