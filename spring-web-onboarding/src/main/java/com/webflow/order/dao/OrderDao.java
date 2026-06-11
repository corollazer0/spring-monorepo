package com.webflow.order.dao;

import com.webflow.order.domain.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
