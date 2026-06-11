package com.webflow.step01.example;

import com.webflow.product.dao.ProductDao;
import com.webflow.product.domain.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Web Step 1 — example] 상품 DAO + 시드 봉인 (@MybatisTest — TestCraft Step 3 복습)
 *
 * 시드 기준값(이후 모든 Step의 출발선):
 * 상품 12종 = KEYBOARD 5(품절 1) + MOUSE 4 + MONITOR 3 / 최저가 35000(무선 마우스)
 *
 * 그리고 이 모듈의 첫 신무기 — decreaseStock의 "원자적 차감" 검증:
 * 확인과 차감이 한 UPDATE라서 동시 주문에도 재고가 음수가 되지 않는다.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("상품 DAO + 시드 봉인")
class ProductDaoTest {

    @Autowired
    private ProductDao productDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("시드 봉인: 12종 / KEYBOARD 5 / 품절 1")
    void seed_상품분포_고정시나리오() {
        assertThat(count("SELECT COUNT(*) FROM product")).isEqualTo(12);
        assertThat(count("SELECT COUNT(*) FROM product WHERE category = 'KEYBOARD'")).isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM product WHERE stock = 0")).isEqualTo(1);
    }

    @Test
    @DisplayName("재고가 충분하면 차감 성공(affected 1) — 재조회로 잔량까지 확인")
    void decreaseStock_재고충분_차감성공() {
        // when : 재고 10인 상품 1에서 3 차감
        int affected = productDao.decreaseStock(1L, 3);

        // then : 쓰기 검증은 재조회로 완성 (TestCraft Step 3의 철학)
        assertThat(affected).isEqualTo(1);
        Product product = productDao.findById(1L);
        assertThat(product.getStock()).isEqualTo(7);
    }

    @Test
    @DisplayName("품절 상품 차감은 affected 0 — 재고는 음수가 되지 않는다 (원자적 차감의 가치)")
    void decreaseStock_품절상품_차감실패() {
        // when : 재고 0인 상품 5(게이밍 키보드)에서 1 차감 시도
        int affected = productDao.decreaseStock(5L, 1);

        // then : WHERE stock >= 수량 조건이 차감을 거부했다 — SELECT 후 UPDATE로
        //        나눴다면 동시 요청 사이로 음수 재고가 끼어들 수 있었다!
        assertThat(affected).isZero();
        assertThat(productDao.findById(5L).getStock()).isZero(); // 그대로!
    }

    private Integer count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
