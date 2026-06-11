package com.webflow.product.service;

import com.webflow.common.dto.PageResponse;
import com.webflow.common.exception.ProductNotFoundException;
import com.webflow.product.dao.ProductDao;
import com.webflow.product.domain.Product;
import com.webflow.product.dto.ProductCreateRequest;
import com.webflow.product.dto.ProductResponse;
import com.webflow.product.dto.ProductSearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 상품 서비스 (캐싱은 Step 6에서 확장).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    /** 정렬 화이트리스트 — 여기 없는 키는 400으로 거절한다 (인젝션 방어의 1차 관문) */
    private static final Set<String> ALLOWED_SORTS =
            new HashSet<>(Arrays.asList("recent", "priceAsc", "priceDesc", "name"));

    private final ProductDao productDao;

    public ProductResponse getProduct(Long productId) {
        return ProductResponse.from(findProductOrThrow(productId));
    }

    /**
     * 목록 검색 — page는 1부터. sort는 화이트리스트 검증 후 XML의 <choose>가 선택한다.
     */
    public PageResponse<ProductResponse> getProducts(ProductSearchCondition condition,
                                                     int page, int size) {
        if (condition.getSort() == null || condition.getSort().isEmpty()) {
            condition.setSort("recent");
        }
        if (!ALLOWED_SORTS.contains(condition.getSort())) {
            throw new IllegalArgumentException(
                    "지원하지 않는 정렬입니다: " + condition.getSort() + " (허용: " + ALLOWED_SORTS + ")");
        }

        int offset = (page - 1) * size;
        List<ProductResponse> content = productDao.search(condition, offset, size).stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
        long totalCount = productDao.countBySearch(condition);

        return PageResponse.of(content, page, size, totalCount);
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
