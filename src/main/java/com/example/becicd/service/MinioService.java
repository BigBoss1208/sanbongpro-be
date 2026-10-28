package com.example.becicd.service;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {

    @Value("${minio.bucket:${minio.bucket-name:sanbong-files}}")
    private String bucketName;

    @Value("${minio.url:${minio.endpoint:http://localhost:9000}}")
    private String minioUrl;

    private final MinioClient minioClient;

    @org.springframework.beans.factory.annotation.Autowired
    public MinioService(org.springframework.beans.factory.ObjectProvider<MinioClient> clients) {
        this.minioClient = clients.getIfAvailable();
    }

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    // 1. Upload 1 tham số (mặc định)
    public String uploadFile(MultipartFile file) throws Exception {
        return upload(file, "general");
    }

    // 2. Overload upload 1 tham số
    public String upload(MultipartFile file) throws Exception {
        return upload(file, "general");
    }

    // 3. Overload upload 2 tham số (được gọi từ ProductService: upload(image, "products"))
    public String upload(MultipartFile file, String folder) throws Exception {
        requireStorage();
        if (file == null || file.isEmpty() || file.getSize() > 10 * 1024 * 1024) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "File rỗng hoặc vượt quá 10 MB");
        if (!folder.matches("[a-zA-Z0-9_-]+")) throw new IllegalArgumentException("Thư mục không hợp lệ");
        if (folder.equals("san") || folder.equals("products")) validateImage(file);
        String extension = "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        if (folder.equals("san") || folder.equals("products")) extension = "image/jpeg".equals(file.getContentType()) ? ".jpg" : "image/png".equals(file.getContentType()) ? ".png" : ".webp";
        String fileName = folder + "/" + UUID.randomUUID().toString() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }

        return folder.equals("general") ? getPresignedUrl(fileName) : fileName;
    }

    // 4. Download file Stream
    public InputStream download(String objectName) throws Exception {
        requireStorage();
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey(objectName))
                        .build()
        );
    }

    // 5. Lấy Metadata file
    public StatObjectResponse getFileInfo(String objectName) throws Exception {
        requireStorage();
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey(objectName))
                        .build()
        );
    }

    // 6. Lấy Presigned URL
    public String getPresignedUrl(String objectName) throws Exception {
        requireStorage();
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectKey(objectName))
                        .expiry(15, TimeUnit.MINUTES)
                        .build()
        );
    }
    public String getPublicUrl(String objectName) {
        requireStorage();
        return minioUrl.replaceAll("/+$", "") + "/" + bucketName + "/" + java.util.Arrays.stream(objectKey(objectName).split("/")).map(segment -> java.net.URLEncoder.encode(segment, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20")).collect(java.util.stream.Collectors.joining("/"));
    }
    public String objectKey(String reference) {
        if (reference == null || reference.isBlank()) throw new IllegalArgumentException("Thiếu tên file");
        String key = reference;
        if (reference.startsWith("http://") || reference.startsWith("https://")) {
            java.net.URI uri = java.net.URI.create(reference);
            java.net.URI endpoint = java.net.URI.create(minioUrl);
            if (!java.util.Objects.equals(uri.getScheme(), endpoint.getScheme()) || !java.util.Objects.equals(uri.getAuthority(), endpoint.getAuthority()) || !uri.getPath().startsWith("/" + bucketName + "/")) throw new IllegalArgumentException("URL không thuộc kho lưu trữ");
            key = uri.getPath().substring(bucketName.length() + 2);
        }
        if (key.startsWith(bucketName + "/")) key = key.substring(bucketName.length() + 1);
        if (key.startsWith("/") || key.contains("..") || key.contains("\\") || key.contains(":")) throw new IllegalArgumentException("Tên file không hợp lệ");
        return key;
    }
    private void requireStorage() {
        if (minioClient == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "Upload kho ảnh chưa được bật. Hãy sử dụng URL ảnh HTTPS.");
    }
    private void validateImage(MultipartFile file) throws Exception {
        if (!java.util.List.of("image/jpeg", "image/png", "image/webp").contains(file.getContentType())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ JPEG, PNG, WebP");
        byte[] header;
        try (InputStream stream = file.getInputStream()) { header = stream.readNBytes(12); }
        boolean jpeg = header.length >= 3 && (header[0] & 255) == 255 && (header[1] & 255) == 216 && (header[2] & 255) == 255;
        boolean png = header.length >= 8 && java.util.Arrays.equals(java.util.Arrays.copyOf(header, 8), new byte[]{(byte)137,80,78,71,13,10,26,10});
        boolean webp = header.length == 12 && new String(header, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF") && new String(header, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
        if (!("image/jpeg".equals(file.getContentType()) && jpeg || "image/png".equals(file.getContentType()) && png || "image/webp".equals(file.getContentType()) && webp)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Nội dung ảnh không hợp lệ");
    }
}
