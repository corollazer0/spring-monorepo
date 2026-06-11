package com.webflow.product.dto;

import com.webflow.product.domain.Product;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 상품 응답 DTO.
 */
@Getter
@Builder
public class ProductResponse {

    private final Long productId;
    private final String name;
    private final String category;
    private final long price;
    private final int stock;
    private final boolean soldOut;
    private final LocalDateTime createdAt;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .category(product.getCategory())
                .price(product.getPrice())
                .stock(product.getStock())
                .soldOut(product.getStock() <= 0)
                .createdAt(product.getCreatedAt())
                .build();
    }
}
