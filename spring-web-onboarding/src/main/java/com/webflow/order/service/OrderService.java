package com.webflow.order.service;

import com.webflow.common.exception.BusinessException;
import com.webflow.common.exception.OrderNotFoundException;
import com.webflow.common.exception.OutOfStockException;
import com.webflow.common.exception.PaymentDeclinedException;
import com.webflow.common.exception.ProductNotFoundException;
import com.webflow.external.payment.PaymentApproveRequest;
import com.webflow.external.payment.PaymentApproveResponse;
import com.webflow.external.payment.PaymentClient;
import com.webflow.order.dao.OrderDao;
import com.webflow.order.domain.Order;
import com.webflow.order.dto.OrderCreateRequest;
import com.webflow.order.dto.OrderResponse;
import com.webflow.product.dao.ProductDao;
import com.webflow.product.domain.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 서비스 — Step 1 기본형.
 *
 * 핵심 비즈니스 규칙:
 * 1. 없는 상품 주문 불가 → ProductNotFoundException (404)
 * 2. 재고 부족 시 주문 불가 → OutOfStockException (409)
 *    재고 확인+차감은 DAO의 원자적 UPDATE(decreaseStock) 한 방 — 동시성 안전
 * 3. 생성된 주문은 PENDING_PAYMENT (결제 연동은 Step 3에서 연결!)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderDao orderDao;
    private final ProductDao productDao;
    private final PaymentClient paymentClient;

    public OrderResponse getOrder(Long orderId) {
        Order order = orderDao.findById(orderId);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }
        return OrderResponse.from(order);
    }

    /**
     * @Transactional: 재고 차감과 주문 INSERT는 한 운명 —
     * INSERT가 실패하면 차감도 함께 롤백되어야 재고가 새지 않는다.
     */
    @Transactional
    public OrderResponse placeOrder(OrderCreateRequest request) {
        Product product = productDao.findById(request.getProductId());
        if (product == null) {
            throw new ProductNotFoundException(request.getProductId());
        }

        // 확인+차감을 원자적 UPDATE 한 방에 — affected 0 = 재고 부족
        int affected = productDao.decreaseStock(request.getProductId(), request.getQuantity());
        if (affected == 0) {
            throw new OutOfStockException(request.getProductId(), request.getQuantity());
        }

        Order order = Order.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalPrice(product.getPrice() * request.getQuantity())
                .status(Order.STATUS_PENDING_PAYMENT)
                .build();
        orderDao.insert(order);

        log.info(">>>>> [OrderService] 주문 생성. orderId={}, total={}",
                order.getOrderId(), order.getTotalPrice());
        return OrderResponse.from(orderDao.findById(order.getOrderId()));
    }

    /**
     * 결제 — 외부 PG 승인 후 PAID 전이. (Step 3)
     *
     * 규칙:
     * 1. PENDING_PAYMENT 상태만 결제 가능 — 이중 결제 방지 (외부 호출 전에 차단!)
     * 2. DECLINED이면 주문은 PENDING_PAYMENT 그대로 보존 (다른 카드로 재시도 가능)
     *
     * 의도적으로 @Transactional이 없다: 외부 HTTP 호출을 DB 트랜잭션 안에
     * 가두면 PG가 느려질 때 커넥션을 물고 늘어진다. 상태 전이는 UPDATE 한 문장
     * (그 자체로 원자적)이고, 승인 후에만 실행되므로 트랜잭션 묶음이 필요 없다.
     */
    public OrderResponse payOrder(Long orderId) {
        Order order = orderDao.findById(orderId);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }
        if (!Order.STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            throw new BusinessException(
                    "결제 대기 상태의 주문만 결제할 수 있습니다. 현재 상태: " + order.getStatus());
        }

        PaymentApproveResponse payment = paymentClient.approve(
                new PaymentApproveRequest(orderId, order.getTotalPrice()));

        if (!payment.isApproved()) {
            // 주문은 건드리지 않는다 — PENDING_PAYMENT 보존이 곧 재시도 가능성
            throw new PaymentDeclinedException(orderId, payment.getMessage());
        }

        orderDao.updateStatus(orderId, Order.STATUS_PAID, payment.getPaymentKey());
        log.info(">>>>> [OrderService] 결제 완료. orderId={}, paymentKey={}",
                orderId, payment.getPaymentKey());
        return OrderResponse.from(orderDao.findById(orderId));
    }
}
