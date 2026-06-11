package com.webflow.order.controller;

import com.webflow.order.dto.OrderDeliveryResponse;
import com.webflow.order.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배송 조회 API (Step 9 캡스톤) — 조회 전용이라 컨트롤러를 분리했다.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    /** GET /api/orders/{orderId}/delivery */
    @GetMapping("/{orderId}/delivery")
    public OrderDeliveryResponse getDeliveryStatus(@PathVariable Long orderId) {
        return deliveryService.getDeliveryStatus(orderId);
    }
}
