package com.webflow.step01.answer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webflow.common.exception.ProductNotFoundException;
import com.webflow.product.controller.ProductController;
import com.webflow.product.dto.ProductCreateRequest;
import com.webflow.product.dto.ProductResponse;
import com.webflow.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Web Step 1 — answer] ProductApiExerciseTest 모범답안
 *
 * 채점 포인트:
 * - Security가 없는 모듈이라 @Import(SecurityConfig)/csrf()가 불필요함을 이해했는가
 *   (보안 자동구성은 starter-security가 클래스패스에 있을 때만 활성화된다)
 */
@WebMvcTest(ProductController.class)
@DisplayName("상품 API (모범답안)")
class ProductApiAnswerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("존재하는 상품 조회 → 200 + JSON 필드")
    void getProduct_존재_200() throws Exception {
        // given (TODO 1 답)
        given(productService.getProduct(1L)).willReturn(
                ProductResponse.builder()
                        .productId(1L).name("기계식 키보드 RED")
                        .category("KEYBOARD").price(89000).stock(10)
                        .build());

        // when & then (TODO 2 답)
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("기계식 키보드 RED"))
                .andExpect(jsonPath("$.price").value(89000));
    }

    @Test
    @DisplayName("없는 상품 조회 → 404 + 에러 규약")
    void getProduct_없음_404() throws Exception {
        // given (TODO 3 답)
        given(productService.getProduct(99L)).willThrow(new ProductNotFoundException(99L));

        // when & then (TODO 4 답)
        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다. productId=99"));
    }

    @Test
    @DisplayName("상품 등록 → 201 + Location 헤더")
    void createProduct_정상_201() throws Exception {
        // given (TODO 5 답)
        given(productService.createProduct(any(ProductCreateRequest.class))).willReturn(5L);
        String body = objectMapper.writeValueAsString(
                new ProductCreateRequest("새 키보드", "KEYBOARD", 50000, 10));

        // when & then (TODO 6 답) : Security 없음 → csrf() 불필요!
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/products/5"));
    }

    @Test
    @DisplayName("카테고리 오타 → 400 + fieldErrors")
    void createProduct_카테고리오타_400() throws Exception {
        // when & then (TODO 7 답)
        String body = objectMapper.writeValueAsString(
                new ProductCreateRequest("새 키보드", "KEYBORD", 50000, 10)); // 오타!

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("category"));
    }
}
