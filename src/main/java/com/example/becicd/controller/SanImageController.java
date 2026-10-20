package com.example.becicd.controller;

import com.example.becicd.repository.SanRepository;
import com.example.becicd.service.MinioService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Only exposes the image attached to a public pitch, never arbitrary storage keys. */
@RestController
@RequestMapping("/api/san")
public class SanImageController {
    private final SanRepository repository;
    private final MinioService storage;
    public SanImageController(SanRepository repository, MinioService storage) { this.repository = repository; this.storage = storage; }
    @GetMapping("/{id}/image")
    public ResponseEntity<InputStreamResource> image(@PathVariable Long id) throws Exception {
        var pitch = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân"));
        if (!com.example.becicd.service.BookingGuard.active(pitch)) return ResponseEntity.notFound().build();
        if (pitch.getHinhAnh() == null || pitch.getHinhAnh().isBlank()) return ResponseEntity.notFound().build();
        String key = storage.objectKey(pitch.getHinhAnh());
        String contentType = storage.getFileInfo(key).contentType();
        if (!java.util.List.of("image/jpeg", "image/png", "image/webp").contains(contentType)) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File không phải ảnh được hỗ trợ");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).cacheControl(CacheControl.noCache()).body(new InputStreamResource(storage.download(key)));
    }
}
