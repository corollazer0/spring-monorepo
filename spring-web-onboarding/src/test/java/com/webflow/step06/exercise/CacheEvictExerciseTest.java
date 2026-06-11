package com.webflow.step06.exercise;

import com.webflow.file.FileStorageService;
import com.webflow.order.dao.OrderDao;
import com.webflow.product.dao.ProductDao;
import com.webflow.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

import java.util.Objects;

/**
 * [Web Step 6 — exercise] 이미지 업로드도 캐시를 비워야 한다 — 직접 증명해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (힌트: example의 placeOrder 무효화 테스트와 같은 구조)
 * 3. .\gradlew :spring-web-onboarding:test 로 통과를 확인한다
 *
 * 상황: 운영자가 이미지를 교체했는데 고객에겐 옛 이미지가 계속 보인다면?
 * uploadProductImage의 @CacheEvict가 그 사고를 막는다 — 그 봉인을 작성하라.
 */
@Disabled("과제: docs/web/education/FOR-WebFlow-Step06.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBootTest
@DisplayName("업로드 캐시 무효화 (연습문제)")
class CacheEvictExerciseTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private ProductDao productDao;

    @MockBean
    private OrderDao orderDao;

    @MockBean
    private FileStorageService fileStorageService;   // 진짜 디스크는 안 쓴다

    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames().forEach(name ->
                Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    @Test
    @DisplayName("이미지를 업로드하면 그 상품의 캐시가 비워진다")
    void uploadImage_업로드후_캐시무효화() {
        // given : ① productDao.findById(1L)가 상품을 반환하도록 스텁하세요
        //         ② fileStorageService.store(any())가 "new.jpg"를 반환하도록 스텁하세요
        // TODO 1
        // TODO 2

        // when-1 : ③ getProduct(1L)로 캐시를 데우고, 캐시에 1L 항목이 있는지 확인하세요
        //          (cacheManager.getCache(CacheConfig.PRODUCTS).get(1L))
        // TODO 3

        // when-2 : ④ uploadProductImage(1L, MockMultipartFile)을 호출하세요
        // TODO 4

        // then : ⑤ 캐시의 1L 항목이 null인지 + 재조회 후 findById가 총 3번인지 검증하세요
        //         (캐시 채움 1 + 업로드 내부의 존재 확인 1 + 무효화 후 재조회 1)
        // TODO 5
    }
}
