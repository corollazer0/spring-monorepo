package com.webflow.step05.example;

import com.webflow.common.exception.InvalidFileException;
import com.webflow.common.exception.StoredFileNotFoundException;
import com.webflow.product.controller.ProductController;
import com.webflow.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Web Step 5 — example B] 파일 API의 HTTP 계약 — multipart와 Resource 응답
 *
 * JSON이 아닌 두 가지 HTTP 모양:
 * - 업로드: multipart/form-data — MockMvc는 multipart() + .file()로 흉내낸다
 * - 다운로드: 바이트 본문 + Content-Disposition 헤더 (브라우저의 "저장" 동작을 만드는 헤더!)
 */
@WebMvcTest(ProductController.class)
@DisplayName("상품 이미지 API")
class ProductImageApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("업로드: multipart 파트명 image로 보내면 저장된 경로를 돌려준다")
    void uploadImage_정상_200() throws Exception {
        // given
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", "bytes".getBytes());
        given(productService.uploadProductImage(eq(1L), any()))
                .willReturn("a1b2c3.jpg");

        // when & then
        mockMvc.perform(multipart("/api/products/1/image").file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagePath").value("a1b2c3.jpg"));

        then(productService).should().uploadProductImage(eq(1L), any());
    }

    @Test
    @DisplayName("업로드 거부: 잘못된 파일이면 400 (InvalidFileException → BusinessException 매핑)")
    void uploadImage_잘못된파일_400() throws Exception {
        // given
        MockMultipartFile exe = new MockMultipartFile(
                "image", "malware.exe", "application/octet-stream", "MZ".getBytes());
        given(productService.uploadProductImage(eq(1L), any()))
                .willThrow(new InvalidFileException("허용되지 않는 확장자입니다: exe"));

        // when & then
        mockMvc.perform(multipart("/api/products/1/image").file(exe))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("확장자")));
    }

    @Test
    @DisplayName("다운로드: 바이트 본문 + Content-Disposition attachment 헤더")
    void downloadImage_정상_200() throws Exception {
        // given : 파일명을 가진 Resource — ByteArrayResource는 이름이 없어 익명 클래스로 부여
        byte[] bytes = "image-bytes".getBytes();
        given(productService.getProductImage(1L)).willReturn(new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "a1b2c3.jpg";
            }
        });

        // when & then
        mockMvc.perform(get("/api/products/1/image"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("a1b2c3.jpg")))
                .andExpect(content().bytes(bytes));
    }

    @Test
    @DisplayName("다운로드: 이미지 미등록이면 404")
    void downloadImage_이미지없음_404() throws Exception {
        // given
        given(productService.getProductImage(7L))
                .willThrow(new StoredFileNotFoundException("등록된 이미지가 없습니다. productId=7"));

        // when & then
        mockMvc.perform(get("/api/products/7/image"))
                .andExpect(status().isNotFound());
    }
}
