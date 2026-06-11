package com.webflow.product.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 상품 도메인 — 순수 POJO (MyBatis 매핑용 기본 생성자 + setter).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    private Long productId;
    private String name;
    private String category;   // KEYBOARD / MOUSE / MONITOR
    private long price;
    private int stock;
    private String imagePath;  // Step 5에서 채워진다
    private LocalDateTime createdAt;
}
