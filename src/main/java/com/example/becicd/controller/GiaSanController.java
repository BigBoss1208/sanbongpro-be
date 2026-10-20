package com.example.becicd.controller;

import com.example.becicd.dto.GiaSanDTO;
import com.example.becicd.entity.GiaSan;
import com.example.becicd.repository.GiaSanRepository;
import com.example.becicd.repository.KhungGioRepository;
import com.example.becicd.repository.SanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gia-san")
public class GiaSanController {

    private final GiaSanRepository giaSanRepository;
    private final SanRepository sanRepository;
    private final KhungGioRepository khungGioRepository;

    public GiaSanController(GiaSanRepository giaSanRepository, SanRepository sanRepository, KhungGioRepository khungGioRepository) {
        this.giaSanRepository = giaSanRepository;
        this.sanRepository = sanRepository;
        this.khungGioRepository = khungGioRepository;
    }

    // GET /api/gia-san - lấy toàn bộ bảng giá (san + khungGio + gia)
    @GetMapping
    public ResponseEntity<List<GiaSanDTO>> layDanhSach(@RequestParam(required = false) Long sanId) {
        List<GiaSanDTO> ketQua = (sanId == null ? giaSanRepository.findAll() : giaSanRepository.findBySanIdOrderByKhungGioGioBatDauAsc(sanId)).stream()
                .map(GiaSanDTO::tu)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ketQua);
    }

    // POST - admin/staff set giá cho 1 sân + khung giờ
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GiaSanDTO> taoMoi(@Valid @RequestBody GiaSanRequest request) {
        GiaSan giaSan = new GiaSan();
        capNhat(giaSan, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GiaSanDTO.tu(giaSanRepository.save(giaSan)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GiaSanDTO> capNhat(@PathVariable Long id, @Valid @RequestBody GiaSanRequest request) {
        GiaSan giaSan = giaSanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bảng giá"));
        capNhat(giaSan, request);
        return ResponseEntity.ok(GiaSanDTO.tu(giaSanRepository.save(giaSan)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> xoa(@PathVariable Long id) {
        if (!giaSanRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        giaSanRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void capNhat(GiaSan giaSan, GiaSanRequest request) {
        var san = sanRepository.findById(request.getSanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân"));
        var khungGio = khungGioRepository.findById(request.getKhungGioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khung giờ"));
        String loaiNgay = request.getLoaiNgay() == null ? "TAT_CA" : request.getLoaiNgay().toUpperCase();
        if (!List.of("TAT_CA", "NGAY_THUONG", "CUOI_TUAN").contains(loaiNgay)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại ngày không hợp lệ");
        }
        boolean duplicate = giaSanRepository.findAllBySanIdAndKhungGioId(request.getSanId(), request.getKhungGioId())
                .stream().anyMatch(item -> !item.getId().equals(giaSan.getId())
                        && loaiNgay.equalsIgnoreCase(item.getLoai_ngay() == null ? "TAT_CA" : item.getLoai_ngay()));
        if (duplicate) throw new ResponseStatusException(HttpStatus.CONFLICT, "Sân đã có giá cho khung giờ và loại ngày này");
        giaSan.setSan(san);
        giaSan.setKhungGio(khungGio);
        giaSan.setGia(request.getGia());
        giaSan.setLoai_ngay(loaiNgay);
    }

    public static class GiaSanRequest {
        @NotNull private Long sanId;
        @NotNull private Long khungGioId;
        @NotNull @DecimalMin(value = "0.0", inclusive = false) private BigDecimal gia;
        private String loaiNgay;
        public Long getSanId() { return sanId; }
        public void setSanId(Long sanId) { this.sanId = sanId; }
        public Long getKhungGioId() { return khungGioId; }
        public void setKhungGioId(Long khungGioId) { this.khungGioId = khungGioId; }
        public BigDecimal getGia() { return gia; }
        public void setGia(BigDecimal gia) { this.gia = gia; }
        public String getLoaiNgay() { return loaiNgay; }
        public void setLoaiNgay(String loaiNgay) { this.loaiNgay = loaiNgay; }
    }
}