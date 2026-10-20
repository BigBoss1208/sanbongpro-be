
package com.example.becicd.controller;

import com.example.becicd.dto.SanDTO;
import com.example.becicd.dto.ThemSanRequest;
import com.example.becicd.entity.CumSan;
import com.example.becicd.entity.San;
import com.example.becicd.repository.CumSanRepository;
import com.example.becicd.repository.SanRepository;
import com.example.becicd.service.MinioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/san")
public class SanAdminController {

    private static final Logger log =
            LoggerFactory.getLogger(SanAdminController.class);

    private final SanRepository sanRepository;
    private final CumSanRepository cumSanRepository;
    private final MinioService minioService;

    public SanAdminController(
            SanRepository sanRepository,
            CumSanRepository cumSanRepository,
            MinioService minioService) {
        this.sanRepository = sanRepository;
        this.cumSanRepository = cumSanRepository;
        this.minioService = minioService;
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<SanDTO> danhSachQuanTri() {
        return sanRepository.findAll()
                .stream()
                .map(SanDTO::tu)
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/{id}/image-url")
    public Map<String, String> imageUrl(@PathVariable Long id) throws Exception {
        San san = sanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy sân"));

        if (san.getHinhAnh() == null || san.getHinhAnh().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Sân chưa có ảnh");
        }

        return Map.of("url", minioService.getPublicUrl(san.getHinhAnh()));
    }

    // ================= UPLOAD ẢNH =================

    @PostMapping("/upload-anh")
    public ResponseEntity<?> uploadAnh(
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("File ảnh không được để trống");
        }

        try {
            String objectKey = minioService.upload(file, "san");

            String publicUrl = minioService.getPublicUrl(objectKey);

            log.info("Upload ảnh sân thành công: {}", objectKey);

            return ResponseEntity.ok(Map.of(
                    "url", publicUrl,
                    "objectKey", objectKey
            ));

        } catch (ResponseStatusException ex) {
            throw ex;

        } catch (io.minio.errors.ErrorResponseException ex) {

            String code = ex.errorResponse() != null
                    ? ex.errorResponse().code()
                    : "UNKNOWN";

            int status = ex.response() != null
                    ? ex.response().code()
                    : 0;

            log.error(
                    "MinIO upload rejected: code={}, status={}",
                    code,
                    status,
                    ex
            );

            String message = "SignatureDoesNotMatch".equals(code)
                    ? "Kho ảnh từ chối chữ ký upload. "
                      + "Ảnh cũ được giữ nguyên; "
                      + "cần quản trị MinIO kiểm tra "
                      + "cấu hình xác thực và region."
                    : "Kho ảnh từ chối upload. "
                      + "Ảnh cũ được giữ nguyên.";

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "code", "STORAGE_UPLOAD_REJECTED",
                            "message", message
                    ));

        } catch (Exception ex) {

            log.error("Lỗi upload ảnh lên MinIO", ex);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Không thể tải ảnh lên kho lưu trữ");
        }
    }

    // ================= THÊM SÂN =================

    @PostMapping
    public ResponseEntity<SanDTO> themSan(
            @RequestBody ThemSanRequest request) {

        validateField(request.getTenSan(), request.getLoaiSan());

        San san = new San();

        san.setTenSan(request.getTenSan().trim());
        san.setLoaiSan(request.getLoaiSan());

        san.setHinhAnh(
                request.getHinhAnh() == null
                        || request.getHinhAnh().isBlank()
                        ? null
                        : minioService.objectKey(request.getHinhAnh())
        );

        san.setMoTa(request.getMoTa());
        san.setTrangThai("HOAT_DONG");

        if (request.getCumSanId() != null) {
            CumSan cumSan = cumSanRepository
                    .findById(request.getCumSanId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Không tìm thấy cụm sân"));

            san.setCumSan(cumSan);
        }

        San daLuu = sanRepository.save(san);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SanDTO.tu(daLuu));
    }

    // ================= CẬP NHẬT TRẠNG THÁI =================

    @PutMapping("/{id}/trang-thai")
    public ResponseEntity<SanDTO> capNhatTrangThai(
            @PathVariable Long id,
            @RequestParam String trangThai) {

        San san = sanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sân"));

        if (!java.util.List.of("HOAT_DONG", "TAM_NGUNG")
                .contains(trangThai)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái sân không hợp lệ");
        }

        san.setTrangThai(trangThai);

        return ResponseEntity.ok(
                SanDTO.tu(sanRepository.save(san))
        );
    }

    // ================= SỬA SÂN =================

    @PutMapping("/{id}")
    public ResponseEntity<SanDTO> suaSan(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        San san = sanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy sân"));

        if (request.containsKey("tenSan")) {

            String name = request.get("tenSan") == null
                    ? null
                    : request.get("tenSan").toString();

            if (name == null || name.isBlank() || name.length() > 100) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tên sân bắt buộc, tối đa 100 ký tự");
            }

            san.setTenSan(name.trim());
        }

        if (request.get("loaiSan") != null) {

            validateField(
                    san.getTenSan(),
                    String.valueOf(request.get("loaiSan"))
            );

            san.setLoaiSan(
                    String.valueOf(request.get("loaiSan")).trim()
            );
        }

        if (request.containsKey("hinhAnh")) {

            String image = request.get("hinhAnh") == null
                    ? null
                    : request.get("hinhAnh").toString();

            san.setHinhAnh(
                    image == null || image.isBlank()
                            ? null
                            : minioService.objectKey(image)
            );
        }

        if (request.get("moTa") != null) {
            san.setMoTa(String.valueOf(request.get("moTa")));
        }

        if (request.containsKey("cumSanId")) {

            Object raw = request.get("cumSanId");

            Long clusterId;

            try {
                clusterId = raw == null
                        ? null
                        : Long.valueOf(raw.toString());

            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Mã cụm sân không hợp lệ");
            }

            san.setCumSan(
                    clusterId == null
                            ? null
                            : cumSanRepository.findById(clusterId)
                              .orElseThrow(() ->
                                           new ResponseStatusException(
                                                   HttpStatus.NOT_FOUND,
                                                   "Không tìm thấy cụm sân"))
            );
        }

        return ResponseEntity.ok(
                SanDTO.tu(sanRepository.save(san))
        );
    }

    // ================= VALIDATE =================

    private void validateField(String name, String type) {

        if (name == null
                || name.isBlank()
                || name.trim().length() > 100) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên sân bắt buộc, tối đa 100 ký tự");
        }

        if (type != null && type.trim().length() > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Loại sân tối đa 20 ký tự");
        }
    }

    // ================= XÓA SÂN =================

    @DeleteMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> xoaSan(@PathVariable Long id) {

        if (!sanRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        San san = sanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND));

        if (san.getDanhSachDatSan() != null
                && !san.getDanhSachDatSan().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Sân có lịch sử đặt; hãy tạm ngưng thay vì xóa");
        }

        if (san.getDanhSachGia() != null
                && !san.getDanhSachGia().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Xóa bảng giá liên quan trước khi xóa sân");
        }

        sanRepository.delete(san);

        return ResponseEntity.noContent().build();
    }
}
