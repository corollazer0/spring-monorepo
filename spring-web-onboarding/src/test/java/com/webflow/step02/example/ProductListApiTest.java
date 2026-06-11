package com.webflow.step02.example;

import com.webflow.product.controller.ProductController;
import com.webflow.product.dto.ProductResponse;
import com.webflow.product.dto.ProductSearchCondition;
import com.webflow.common.dto.PageResponse;
import com.webflow.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Web Step 2 — example B] 목록 API의 HTTP 계약 — PageResponse 규약과 파라미터 바인딩
 */
@WebMvcTest(ProductController.class)
@DisplayName("상품 목록 API")
class ProductListApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("PageResponse 규약: content/page/size/totalCount/totalPages가 응답에 있다")
    void getProducts_기본요청_페이지규약() throws Exception {
        // given
        given(productService.getProducts(any(ProductSearchCondition.class), eq(1), eq(5)))
                .willReturn(PageResponse.of(
                        Arrays.asList(
                                ProductResponse.builder().productId(12L).name("휴대용 모니터").price(180000).build(),
                                ProductResponse.builder().productId(11L).name("32인치 모니터").price(410000).build()),
                        1, 5, 12));

        // when & then : 화면이 페이지네이션을 그리는 데 필요한 모든 필드 = 깨지면 안 되는 계약
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("휴대용 모니터"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalCount").value(12))
                .andExpect(jsonPath("$.totalPages").value(3)); // ceil(12/5)
    }

    @Test
    @DisplayName("쿼리 파라미터가 검색 조건으로 정확히 바인딩되어 서비스에 전달된다")
    void getProducts_파라미터_바인딩검증() throws Exception {
        // given
        given(productService.getProducts(any(ProductSearchCondition.class), anyInt(), anyInt()))
                .willReturn(PageResponse.of(Arrays.asList(), 2, 3, 0));

        // when
        mockMvc.perform(get("/api/products")
                        .param("page", "2").param("size", "3")
                        .param("keyword", "키보드")
                        .param("category", "KEYBOARD")
                        .param("sort", "priceAsc"))
                .andExpect(status().isOk());

        // then : 조건 객체의 "내용물"까지 — Captor (TestCraft Step 2의 무기)
        ArgumentCaptor<ProductSearchCondition> captor =
                ArgumentCaptor.forClass(ProductSearchCondition.class);
        verify(productService).getProducts(captor.capture(), eq(2), eq(3));

        ProductSearchCondition condition = captor.getValue();
        assertThat(condition.getKeyword()).isEqualTo("키보드");
        assertThat(condition.getCategory()).isEqualTo("KEYBOARD");
        assertThat(condition.getSort()).isEqualTo("priceAsc");
    }

    @Test
    @DisplayName("화이트리스트에 없는 sort → 400 (서비스 검증이 advice로 번역된다)")
    void getProducts_잘못된sort_400() throws Exception {
        // given : Service가 화이트리스트 검증에서 거부하는 상황
        given(productService.getProducts(any(ProductSearchCondition.class), anyInt(), anyInt()))
                .willThrow(new IllegalArgumentException("지원하지 않는 정렬입니다: price; DROP TABLE"));

        // when & then : 인젝션 시도 같은 입력은 문 앞(400)에서 끊긴다
        mockMvc.perform(get("/api/products").param("sort", "price; DROP TABLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("지원하지 않는 정렬")));
    }
}
