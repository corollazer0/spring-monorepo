package com.webflow.order.service;

import com.webflow.common.exception.BusinessException;
import com.webflow.common.exception.OrderNotFoundException;
import com.webflow.external.delivery.DeliveryClient;
import com.webflow.external.delivery.DeliveryStatusResponse;
import com.webflow.order.dao.OrderDao;
import com.webflow.order.domain.Order;
import com.webflow.order.dto.OrderDeliveryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 배송 조회 서비스 (Step 9 캡스톤).
 *
 * 결제(payOrder)와 같은 골격 — 외부 호출 "전" 방어선이 먼저다:
 * 1. 없는 주문 → 404 (배송사 호출 없이)
 * 2. PAID가 아닌 주문 → 400 (미결제·취소 주문의 배송이란 존재하지 않는다)
 * 통과한 주문만 배송사에 묻는다. paymentKey가 곧 배송사와의 연결 고리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final OrderDao orderDao;
    private final DeliveryClient deliveryClient;

    public OrderDeliveryResponse getDeliveryStatus(Long orderId) {
        Order order = orderDao.findById(orderId);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }
        if (!Order.STATUS_PAID.equals(order.getStatus())) {
            throw new BusinessException(
                    "결제 완료된 주문만 배송 조회가 가능합니다. 현재 상태: " + order.getStatus());
        }

        DeliveryStatusResponse delivery = deliveryClient.track(order.getPaymentKey());
        log.info(">>>>> [DeliveryService] 배송 조회 완료. orderId={}, status={}",
                orderId, delivery.getStatus());
        return OrderDeliveryResponse.of(orderId, delivery);
    }
}
