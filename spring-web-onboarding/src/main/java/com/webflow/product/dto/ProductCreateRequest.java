package com.webflow.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 상품 등록 요청 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductCreateRequest {

    @NotBlank(message = "상품명은 필수입니다")
    @Size(max = 100, message = "상품명은 100자 이하여야 합니다")
    private String name;

    @NotBlank(message = "카테고리는 필수입니다")
    @Pattern(regexp = "KEYBOARD|MOUSE|MONITOR", message = "카테고리는 KEYBOARD/MOUSE/MONITOR 중 하나여야 합니다")
    private String category;

    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private long price;

    @Min(value = 0, message = "재고는 0 이상이어야 합니다")
    private int stock;

    public ProductCreateRequest(String name, String category, long price, int stock) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }
}
