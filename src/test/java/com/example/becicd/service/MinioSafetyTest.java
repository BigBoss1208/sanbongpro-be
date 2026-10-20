package com.example.becicd.service;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class MinioSafetyTest {
    @Test void normalizesOwnedLegacyUrlsAndRejectsForeignPaths() {
        MinioService service = new MinioService(mock(MinioClient.class));
        ReflectionTestUtils.setField(service, "bucketName", "test-bucket"); ReflectionTestUtils.setField(service, "minioUrl", "https://storage.example.test");
        assertEquals("products/photo.png", service.objectKey("https://storage.example.test/test-bucket/products/photo.png"));
        assertEquals("products/photo.png", service.objectKey("products/photo.png"));
        assertThrows(IllegalArgumentException.class, () -> service.objectKey("https://foreign.test/test-bucket/private.pdf"));
        assertThrows(IllegalArgumentException.class, () -> service.objectKey("../private.pdf"));
    }
    @Test void rejectsSpoofedImageBeforeStorageCall() {
        MinioClient client = mock(MinioClient.class); MinioService service = new MinioService(client);
        assertThrows(Exception.class, () -> service.upload(new MockMultipartFile("file", "fake.png", "image/png", "not-an-image".getBytes()), "san"));
        verifyNoInteractions(client);
    }
    @Test void uploadsToExistingBucketWithoutRegionOrBucketCreationCalls() throws Exception {
        MinioClient client = mock(MinioClient.class); MinioService service = new MinioService(client);
        ReflectionTestUtils.setField(service, "bucketName", "nhom2");
        ReflectionTestUtils.setField(service, "minioUrl", "http://103.167.88.223:9000");
        byte[] image = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aXs8AAAAASUVORK5CYII=");
        String key = service.upload(new MockMultipartFile("file", "pitch.png", "image/png", image), "san");
        assertTrue(key.startsWith("san/"));
        assertEquals("http://103.167.88.223:9000/nhom2/" + key, service.getPublicUrl("nhom2/" + key));
        verify(client).putObject(any(io.minio.PutObjectArgs.class));
        verify(client, never()).bucketExists(any(io.minio.BucketExistsArgs.class));
        verify(client, never()).makeBucket(any(io.minio.MakeBucketArgs.class));
        assertThrows(Exception.class, () -> service.upload(new MockMultipartFile("file", "large.png", "image/png", new byte[10 * 1024 * 1024 + 1]), "san"));
    }
}
