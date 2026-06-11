package com.webflow.step06.answer;

import com.webflow.config.CacheConfig;
import com.webflow.file.FileStorageService;
import com.webflow.order.dao.OrderDao;
import com.webflow.product.dao.ProductDao;
import com.webflow.product.domain.Product;
import com.webflow.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * [Web Step 6 — answer] CacheEvictExerciseTest 모범답안
 *
 * 채점 포인트: "캐시 항목 소멸"과 "재조회 시 DB 재호출" 둘 다 검증했는가 —
 * 전자는 상태, 후자는 행동. 둘이 합쳐져야 무효화가 완전히 봉인된다.
 */
@SpringBootTest
@DisplayName("업로드 캐시 무효화 (모범답안)")
class CacheEvictAnswerTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private ProductDao productDao;

    @MockBean
    private OrderDao orderDao;

    @MockBean
    private FileStorageService fileStorageService;

    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames().forEach(name ->
                Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    @Test
    @DisplayName("이미지를 업로드하면 그 상품의 캐시가 비워진다")
    void uploadImage_업로드후_캐시무효화() {
        // given (TODO 1, 2 답)
        given(productDao.findById(1L)).willReturn(Product.builder()
                .productId(1L).name("기계식 키보드 RED").category("KEYBOARD")
                .price(89000).stock(10)
                .build());
        given(fileStorageService.store(any())).willReturn("new.jpg");

        // when-1 (TODO 3 답) : 캐시를 데운다
        productService.getProduct(1L);
        assertThat(Objects.requireNonNull(cacheManager.getCache(CacheConfig.PRODUCTS)).get(1L))
                .isNotNull();

        // when-2 (TODO 4 답)
        productService.uploadProductImage(1L, new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", "bytes".getBytes()));

        // then (TODO 5 답) : 상태(항목 소멸) + 행동(DB 재호출) 모두
        assertThat(Objects.requireNonNull(cacheManager.getCache(CacheConfig.PRODUCTS)).get(1L))
                .isNull();
        productService.getProduct(1L);
        verify(productDao, times(3)).findById(1L);   // 캐시채움 1 + upload 내부 존재확인 1 + 재조회 1
    }
}
