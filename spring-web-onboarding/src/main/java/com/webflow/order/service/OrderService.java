package com.webflow.order.service;

import com.webflow.common.exception.OrderNotFoundException;
import com.webflow.common.exception.OutOfStockException;
import com.webflow.common.exception.ProductNotFoundException;
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
}
