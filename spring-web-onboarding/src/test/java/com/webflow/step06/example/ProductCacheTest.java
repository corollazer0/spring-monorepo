package com.webflow.step06.example;

import com.webflow.common.exception.ProductNotFoundException;
import com.webflow.config.CacheConfig;
import com.webflow.order.dao.OrderDao;
import com.webflow.order.domain.Order;
import com.webflow.order.dto.OrderCreateRequest;
import com.webflow.order.service.OrderService;
import com.webflow.product.dao.ProductDao;
import com.webflow.product.domain.Product;
import com.webflow.product.dto.ProductResponse;
import com.webflow.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * [Web Step 6 — example] 캐시 동작 검증 — "DB가 몇 번 불렸나"
 *
 * 왜 @SpringBootTest인가: @Cacheable은 프록시(AOP)가 하는 일이라
 * new ProductService(...)로는 캐시가 아예 동작하지 않는다 — 컨테이너가 필요하다.
 *
 * 검증의 핵심 도구는 @MockBean DAO + verify(times(N)):
 * "응답이 같다"로는 캐시를 증명 못 한다(매번 DB를 불러도 응답은 같으니까!).
 * "DAO가 한 번만 불렸다"가 캐시 히트의 유일한 증거다.
 *
 * 주의: 캐시는 테스트 사이에 "살아남는다" — @BeforeEach에서 반드시 비운다
 * (BatchFlow의 @AfterEach 원상복구와 같은 철학: 실행 순서 의존 제거).
 */
@SpringBootTest
@DisplayName("상품 캐시")
class ProductCacheTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private ProductDao productDao;

    @MockBean
    private OrderDao orderDao;

    @BeforeEach
    void clearCache() {
        // @MockBean은 자동 리셋되지만 캐시는 아니다! — 직접 비워 테스트 격리
        cacheManager.getCacheNames().forEach(name ->
                Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    private Product product(Long id, int stock) {
        return Product.builder()
                .productId(id).name("기계식 키보드 RED").category("KEYBOARD")
                .price(89000).stock(stock)
                .build();
    }

    @Test
    @DisplayName("같은 ID 두 번 조회 — DB는 한 번만 불린다 (캐시 히트의 증거)")
    void getProduct_같은ID두번_DB한번() {
        // given
        given(productDao.findById(1L)).willReturn(product(1L, 10));

        // when
        ProductResponse first = productService.getProduct(1L);
        ProductResponse second = productService.getProduct(1L);

        // then : 응답 비교가 아니라 "호출 횟수"가 캐시의 증거다
        assertThat(first.getName()).isEqualTo(second.getName());
        verify(productDao, times(1)).findById(1L);
    }

    @Test
    @DisplayName("다른 ID는 각자 캐시 — key가 productId라서")
    void getProduct_다른ID_키별캐시() {
        // given
        given(productDao.findById(1L)).willReturn(product(1L, 10));
        given(productDao.findById(2L)).willReturn(product(2L, 5));

        // when : 1번 두 번, 2번 한 번
        productService.getProduct(1L);
        productService.getProduct(1L);
        productService.getProduct(2L);

        // then
        verify(productDao, times(1)).findById(1L);
        verify(productDao, times(1)).findById(2L);
    }

    @Test
    @DisplayName("주문하면 그 상품의 캐시가 비워진다 — 재고가 바뀌었으니까 (@CacheEvict)")
    void placeOrder_주문후_캐시무효화() {
        // given : 캐시를 데워둔다
        given(productDao.findById(1L)).willReturn(product(1L, 10));
        given(productDao.decreaseStock(anyLong(), anyInt())).willReturn(1);
        given(orderDao.findById(any())).willReturn(
                Order.builder().orderId(100L).productId(1L).quantity(1)
                        .totalPrice(89000).status(Order.STATUS_PENDING_PAYMENT).build());
        productService.getProduct(1L);
        assertThat(Objects.requireNonNull(cacheManager.getCache(CacheConfig.PRODUCTS)).get(1L))
                .isNotNull();   // 캐시에 들어있다

        // when : 주문 — 재고가 바뀐다
        orderService.placeOrder(new OrderCreateRequest(1L, 1));

        // then : 캐시 항목이 사라졌고, 다음 조회는 DB로 간다
        assertThat(Objects.requireNonNull(cacheManager.getCache(CacheConfig.PRODUCTS)).get(1L))
                .isNull();
        productService.getProduct(1L);
        verify(productDao, times(3)).findById(1L);   // 캐시채움 1 + placeOrder 내부 1 + 재조회 1
    }

    @Test
    @DisplayName("예외는 캐시되지 않는다 — 없는 상품은 매번 DB로 간다")
    void getProduct_없는상품_예외는캐시안됨() {
        // given
        given(productDao.findById(99L)).willReturn(null);

        // when : 두 번 다 404
        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(ProductNotFoundException.class);

        // then : 캐시 히트가 없었다 — "없음"이 캐시되면 새로 등록돼도 계속 404가 된다!
        verify(productDao, times(2)).findById(99L);
    }
}
