package com.webflow.product.service;

import com.webflow.common.dto.PageResponse;
import com.webflow.common.exception.ProductNotFoundException;
import com.webflow.common.exception.StoredFileNotFoundException;
import com.webflow.config.CacheConfig;
import com.webflow.file.FileStorageService;
import com.webflow.product.dao.ProductDao;
import com.webflow.product.domain.Product;
import com.webflow.product.dto.ProductCreateRequest;
import com.webflow.product.dto.ProductResponse;
import com.webflow.product.dto.ProductSearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    private final FileStorageService fileStorageService;

    /**
     * 단건 조회 — 캐시 적용 (Step 6).
     * 같은 productId의 두 번째 조회부터는 이 메서드 본문이 "실행되지 않는다" (DB 0회).
     * 예외(없는 상품)는 캐시되지 않는다 — 다음 조회도 DB로 간다.
     */
    @Cacheable(cacheNames = CacheConfig.PRODUCTS, key = "#productId")
    public ProductResponse getProduct(Long productId) {
        log.debug(">>>>> [DEBUG] 캐시 미스 — DB 조회. productId={}", productId);
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

    /**
     * 상품 이미지 업로드 (Step 5) — 검증·저장은 FileStorageService, 경로 기록은 여기서.
     * 순서 주의: 상품 존재 확인이 먼저다 — 없는 상품 때문에 고아 파일을 만들지 않는다.
     * 상품이 바뀌었으니 캐시도 비운다 (Step 6) — 무효화 없는 캐시는 거짓말 제조기.
     */
    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, key = "#productId")
    public String uploadProductImage(Long productId, MultipartFile file) {
        findProductOrThrow(productId);
        String storedName = fileStorageService.store(file);
        productDao.updateImagePath(productId, storedName);
        log.info(">>>>> [ProductService] 이미지 등록. productId={}, imagePath={}", productId, storedName);
        return storedName;
    }

    /** 상품 이미지 다운로드 (Step 5) */
    public Resource getProductImage(Long productId) {
        Product product = findProductOrThrow(productId);
        if (product.getImagePath() == null) {
            throw new StoredFileNotFoundException("등록된 이미지가 없습니다. productId=" + productId);
        }
        return fileStorageService.loadAsResource(product.getImagePath());
    }

    private Product findProductOrThrow(Long productId) {
        Product product = productDao.findById(productId);
        if (product == null) {
            throw new ProductNotFoundException(productId);
        }
        return product;
    }
}
