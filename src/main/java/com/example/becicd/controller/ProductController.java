package com.example.becicd.controller;

import com.example.becicd.dto.ProductRequest;
import com.example.becicd.entity.Product;
import com.example.becicd.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")

public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // 1. Lấy tất cả sản phẩm
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.findAll());
    }

    // 2. Lấy chi tiết sản phẩm theo ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findStoredById(id));
    }

    // 3. Lấy ảnh sản phẩm (Đã thêm throws Exception)
    @GetMapping("/{id}/image")
    public ResponseEntity<InputStreamResource> getImage(@PathVariable Long id) throws Exception {
        Product product = productService.findStoredById(id);
        if (product.getImageUrl() == null || product.getImageUrl().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        InputStream image = productService.downloadImage(product.getImageUrl());
        return ResponseEntity.ok()
                .contentType(productService.getImageContentType(product.getImageUrl()))
                .body(new InputStreamResource(image));
    }

    // 4. Tạo sản phẩm mới kèm ảnh (Đã thêm throws Exception)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new product")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN_SAN', 'NHAN_VIEN_PAGE')")
    public ResponseEntity<Product> create(@Valid @ModelAttribute ProductRequest request) throws Exception {
        Product created = productService.create(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // 5. Cập nhật thông tin sản phẩm
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN_SAN', 'NHAN_VIEN_PAGE')")
    public ResponseEntity<Product> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    // 6. Cập nhật ảnh sản phẩm (Đã thêm throws Exception)
    @PutMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN_SAN', 'NHAN_VIEN_PAGE')")
    public ResponseEntity<Product> updateProductImage(@PathVariable Long id, @ModelAttribute ProductRequest request) throws Exception {
        return ResponseEntity.ok(productService.updateImage(id, request.getImage()));
    }

    // 7. Xóa sản phẩm
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm thành công!"));
    }
}
