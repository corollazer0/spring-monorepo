package com.webflow.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상품 검색 조건 — 동적 SQL(<if>)의 파라미터.
 * sort는 화이트리스트 키("recent"/"priceAsc"/"priceDesc"/"name")만 허용된다 (Service에서 검증).
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductSearchCondition {

    private String keyword;   // 상품명 부분 일치
    private String category;  // 정확히 일치
    private String sort;      // 화이트리스트 키 (null이면 recent)

    public ProductSearchCondition(String keyword, String category, String sort) {
        this.keyword = keyword;
        this.category = category;
        this.sort = sort;
    }
}
