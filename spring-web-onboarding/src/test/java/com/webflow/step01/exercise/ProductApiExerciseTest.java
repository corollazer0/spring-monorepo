package com.webflow.step01.exercise;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webflow.product.controller.ProductController;
import com.webflow.product.service.ProductService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * [Web Step 1 — exercise] 상품 API의 HTTP 계약을 직접 검증해보세요 (TestCraft Step 4 복습)
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다
 * 3. .\gradlew :spring-web-onboarding:test 로 통과를 확인한다
 *
 * 참고: 이 모듈에는 Spring Security가 없다! —
 * TestCraft에서 필수였던 @Import(SecurityConfig)와 with(csrf())가 필요 없는 이유를
 * 설명할 수 있다면 TestCraft를 제대로 졸업한 것입니다.
 */
@Disabled("과제: docs/web/education/FOR-WebFlow-Step01.md 참고 후 @Disabled를 제거하고 완성하세요")
@WebMvcTest(ProductController.class)
@DisplayName("상품 API (연습문제)")
class ProductApiExerciseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("존재하는 상품 조회 → 200 + JSON 필드")
    void getProduct_존재_200() throws Exception {
        // given : productService.getProduct(1L)이 ProductResponse를 돌려주도록 stubbing
        //         (힌트: ProductResponse.builder().productId(1L).name("기계식 키보드 RED")
        //                .price(89000).stock(10).build())
        // TODO 1

        // when & then : GET /api/products/1 → 200 + jsonPath로 name/price 검증
        // TODO 2
    }

    @Test
    @DisplayName("없는 상품 조회 → 404 + 에러 규약")
    void getProduct_없음_404() throws Exception {
        // given : getProduct(99L)가 ProductNotFoundException을 던지도록 stubbing
        // TODO 3

        // when & then : 404 + $.message에 "99" 포함 검증
        // TODO 4
    }

    @Test
    @DisplayName("상품 등록 → 201 + Location 헤더")
    void createProduct_정상_201() throws Exception {
        // given : createProduct가 5L을 돌려주도록 stubbing +
        //         new ProductCreateRequest("새 키보드", "KEYBOARD", 50000, 10)을 JSON으로
        // TODO 5

        // when & then : POST → 201 + Location "/api/products/5"
        //               (csrf()가 필요 없다 — Security가 없는 모듈!)
        // TODO 6
    }

    @Test
    @DisplayName("카테고리 오타 → 400 + fieldErrors")
    void createProduct_카테고리오타_400() throws Exception {
        // when & then : category "KEYBORD"(오타)로 등록 시도 → 400 +
        //               $.fieldErrors[0].field 가 "category"인지 검증
        // TODO 7
    }
}
