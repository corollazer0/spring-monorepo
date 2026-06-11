package com.webflow.order.service;

import com.webflow.config.CacheConfig;
import com.webflow.order.dao.OrderDao;
import com.webflow.order.domain.Order;
import com.webflow.product.dao.ProductDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 미결제 주문 정리 (Step 7).
 *
 * 핵심 설계: cutoff(기준 시각)를 "파라미터로 받는다" —
 * 메서드 안에서 LocalDateTime.now()를 부르면 테스트가 실행 시각에 휘둘린다.
 * 시각은 호출자(스케줄러)가 주입하고, 로직은 순수하게 — 테스트 가능성의 기본기.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCleanupService {

    private final OrderDao orderDao;
    private final ProductDao productDao;

    /**
     * cutoff 이전의 미결제 주문을 취소하고 잠긴 재고를 되돌린다.
     *
     * @Transactional: 취소와 재고 복원은 한 운명 — 취소만 되고 복원이 빠지면
     * 재고가 유령 주문에 영원히 잠긴다 (placeOrder의 정확한 역연산이어야 한다).
     *
     * @CacheEvict(allEntries): 여러 상품의 재고가 한꺼번에 바뀐다 —
     * 키를 일일이 못 짚는 일괄 변경은 전체 비우기가 정직하다.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, allEntries = true)
    public int cancelStaleOrders(LocalDateTime cutoff) {
        List<Order> staleOrders = orderDao.findStaleOrders(cutoff);

        for (Order order : staleOrders) {
            orderDao.updateStatus(order.getOrderId(), Order.STATUS_CANCELLED, null);
            productDao.restoreStock(order.getProductId(), order.getQuantity());
            log.info(">>>>> [OrderCleanupService] 미결제 주문 취소. orderId={}, productId={}, 복원수량={}",
                    order.getOrderId(), order.getProductId(), order.getQuantity());
        }

        log.info(">>>>> [OrderCleanupService] {} 건 정리 완료 (cutoff={})", staleOrders.size(), cutoff);
        return staleOrders.size();
    }
}
