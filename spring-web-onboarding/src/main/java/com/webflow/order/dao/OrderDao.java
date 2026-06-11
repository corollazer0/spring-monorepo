package com.webflow.order.dao;

import com.webflow.order.domain.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 DAO — 구현은 resources/mybatis/mapper/OrderMapper.xml
 */
@Mapper
public interface OrderDao {

    Order findById(Long orderId);

    void insert(Order order);

    /** 상태 전이 (결제 승인 시 paymentKey도 함께) */
    int updateStatus(@Param("orderId") Long orderId,
                     @Param("status") String status,
                     @Param("paymentKey") String paymentKey);

    /** 기준 시각 이전의 미결제 주문 — Step 7 정리 대상 (ordered_at < cutoff, 경계 미포함) */
    List<Order> findStaleOrders(@Param("cutoff") LocalDateTime cutoff);
}
