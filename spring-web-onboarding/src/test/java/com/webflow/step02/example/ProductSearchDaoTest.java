package com.webflow.step02.example;

import com.webflow.product.dao.ProductDao;
import com.webflow.product.domain.Product;
import com.webflow.product.dto.ProductSearchCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Web Step 2 — example A] 검색·정렬·페이징 SQL 검증 (@MybatisTest)
 *
 * 시드 기준값: 12종 / '키보드' 5건 / '기계식' 2건 / MOUSE 4건 / 최저가 35000(무선 마우스)
 *
 * 정렬 검증의 핵심: "정렬됐다"가 아니라 "기대한 순서 그대로"를 확인한다 —
 * 첫 항목과 끝 항목, 또는 전체 순서(extracting + containsExactly)로.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("상품 검색 DAO")
class ProductSearchDaoTest {

    @Autowired
    private ProductDao productDao;

    private ProductSearchCondition condition(String keyword, String category, String sort) {
        return new ProductSearchCondition(keyword, category, sort);
    }

    @Nested
    @DisplayName("동적 검색")
    class Search {

        @Test
        @DisplayName("키워드 '키보드' → 5건, 전부 이름에 키워드 포함")
        void search_키워드_부분일치() {
            // when
            List<Product> result = productDao.search(condition("키보드", null, null), 0, 10);

            // then
            assertThat(result).hasSize(5);
            assertThat(result).allSatisfy(p -> assertThat(p.getName()).contains("키보드"));
        }

        @Test
        @DisplayName("키워드 + 카테고리 복합 조건")
        void search_복합조건() {
            // when : '게이밍'은 KEYBOARD에도 MOUSE에도 있다 — 카테고리로 좁힌다
            List<Product> result = productDao.search(condition("게이밍", "MOUSE", null), 0, 10);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("게이밍 마우스");
        }

        @Test
        @DisplayName("조건 없음 → 전체 (where가 통째로 빠진다)")
        void search_조건없음_전체() {
            assertThat(productDao.countBySearch(condition(null, null, null))).isEqualTo(12);
        }
    }

    @Nested
    @DisplayName("화이트리스트 정렬")
    class Sort {

        @Test
        @DisplayName("priceAsc → 최저가(무선 마우스 35000)가 첫 번째")
        void search_가격오름차순_순서검증() {
            // when
            List<Product> result = productDao.search(condition(null, null, "priceAsc"), 0, 12);

            // then : 양 끝으로 순서를 증명
            assertThat(result.get(0).getName()).isEqualTo("무선 마우스");
            assertThat(result.get(0).getPrice()).isEqualTo(35000);
            assertThat(result.get(11).getPrice()).isEqualTo(410000); // 32인치 모니터
        }

        @Test
        @DisplayName("알 수 없는 sort 키는 기본 정렬(recent: product_id DESC)로 — XML의 otherwise")
        void search_미정의sort_기본정렬() {
            // when : Service 화이트리스트를 통과 못 하는 값이지만, XML 단독으로는
            //        otherwise로 빠진다 — 이중 방어의 안쪽 막
            List<Product> result = productDao.search(condition(null, null, "hack"), 0, 3);

            // then : 최신 등록순 = product_id 큰 순
            assertThat(result.get(0).getProductId()).isEqualTo(12L);
        }
    }

    @Nested
    @DisplayName("페이징 (OFFSET/FETCH)")
    class Paging {

        @Test
        @DisplayName("12건을 5개씩: 1페이지 5 / 2페이지 5 / 3페이지 2")
        void search_페이징_경계검증() {
            ProductSearchCondition all = condition(null, null, null);

            assertThat(productDao.search(all, 0, 5)).hasSize(5);   // page 1
            assertThat(productDao.search(all, 5, 5)).hasSize(5);   // page 2
            assertThat(productDao.search(all, 10, 5)).hasSize(2);  // page 3 — 자투리!
        }
    }
}
