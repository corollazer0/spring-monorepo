package com.webflow.product.service;

import com.webflow.common.exception.ProductNotFoundException;
import com.webflow.product.dao.ProductDao;
import com.webflow.product.domain.Product;
import com.webflow.product.dto.ProductCreateRequest;
import com.webflow.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 상품 서비스 — Step 1 기본형 (검색/페이징은 Step 2, 캐싱은 Step 6에서 확장).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductDao productDao;

    public ProductResponse getProduct(Long productId) {
        return ProductResponse.from(findProductOrThrow(productId));
    }

    public Long createProduct(ProductCreateRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .category(request.getCategory())
                .price(request.getPrice())
                .stock(request.getStock())
                .build();

        productDao.insert(product);
        log.info(">>>>> [ProductService] 상품 등록 완료. productId={}", product.getProductId());
        return product.getProductId();
    }

    private Product findProductOrThrow(Long productId) {
        Product product = productDao.findById(productId);
        if (product == null) {
            throw new ProductNotFoundException(productId);
        }
        return product;
    }
}
