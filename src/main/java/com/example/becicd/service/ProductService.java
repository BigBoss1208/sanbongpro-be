package com.example.becicd.service;

import com.example.becicd.dto.ProductRequest;
import com.example.becicd.entity.Product;
import com.example.becicd.repository.ProductRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final MinioService minioService;

    public ProductService(ProductRepository productRepository, MinioService minioService) {
        this.productRepository = productRepository;
        this.minioService = minioService;
    }

    // 1. Lấy tất cả sản phẩm
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    // 2a. Tìm sản phẩm theo ID (Phương thức chuẩn)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
    }

    // 2b. Method Alias cho findStoredById (Để khớp với nơi đang gọi)
    public Product findStoredById(Long id) {
        return findById(id);
    }

    // 3. Tạo mới sản phẩm kèm upload ảnh lên MinIO
    public Product create(ProductRequest request) throws Exception {
        Product product = new Product(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getQuantity()
        );

        MultipartFile image = request.getImage();
        if (image != null && !image.isEmpty()) {
            String imageUrl = minioService.upload(image, "products");
            product.setImageUrl(imageUrl);
        }

        return productRepository.save(product);
    }

    // 4. Cập nhật thông tin sản phẩm
    public Product update(Long id, ProductRequest request) {
        Product product = findById(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        return productRepository.save(product);
    }

    // 5. Cập nhật/thay đổi ảnh sản phẩm
    public Product updateImage(Long id, MultipartFile image) throws Exception {
        Product product = findById(id);
        if (image != null && !image.isEmpty()) {
            String imageUrl = minioService.upload(image, "products");
            product.setImageUrl(imageUrl);
        }
        return productRepository.save(product);
    }

    // 6. Xóa sản phẩm
    public void delete(Long id) {
        Product product = findById(id);
        productRepository.delete(product);
    }

    // 7. Tải luồng InputStream ảnh sản phẩm từ MinIO
    public InputStream downloadImage(String objectName) throws Exception {
        return minioService.download(objectName);
    }

    // 8. Lấy ContentType của ảnh
    public MediaType getImageContentType(String objectName) {
        try {
            String contentType = minioService.getFileInfo(objectName).contentType();
            return contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
