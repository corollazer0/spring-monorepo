package com.webflow.product.controller;

import com.webflow.common.dto.PageResponse;
import com.webflow.product.dto.ProductCreateRequest;
import com.webflow.product.dto.ProductResponse;
import com.webflow.product.dto.ProductSearchCondition;
import com.webflow.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * 상품 REST API.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 목록 검색 — GET /api/products?page=1&size=5&keyword=키보드&category=KEYBOARD&sort=priceAsc
     */
    @GetMapping
    public PageResponse<ProductResponse> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort) {
        return productService.getProducts(
                new ProductSearchCondition(keyword, category, sort), page, size);
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable Long productId) {
        return productService.getProduct(productId);
    }

    @PostMapping
    public ResponseEntity<Void> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        Long productId = productService.createProduct(request);
        return ResponseEntity.created(URI.create("/api/products/" + productId)).build();
    }

    /** 이미지 업로드 — multipart 파트명 image (Step 5) */
    @PostMapping("/{productId}/image")
    public Map<String, String> uploadImage(@PathVariable Long productId,
                                           @RequestParam("image") MultipartFile image) {
        String imagePath = productService.uploadProductImage(productId, image);
        return Collections.singletonMap("imagePath", imagePath);
    }

    /** 이미지 다운로드 (Step 5) */
    @GetMapping("/{productId}/image")
    public ResponseEntity<Resource> downloadImage(@PathVariable Long productId) {
        Resource image = productService.getProductImage(productId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + image.getFilename())
                .body(image);
    }
}
