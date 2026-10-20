package com.example.becicd.controller;

import com.example.becicd.dto.KhungGioDTO;
import com.example.becicd.entity.KhungGio;
import com.example.becicd.repository.KhungGioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/khung-gio")
public class KhungGioController {

    private final KhungGioRepository khungGioRepository;

    public KhungGioController(KhungGioRepository khungGioRepository) {
        this.khungGioRepository = khungGioRepository;
    }

    // GET /api/khung-gio - lấy tất cả khung giờ (dùng để hiển thị lưới đặt sân)
    @GetMapping
    public ResponseEntity<List<KhungGioDTO>> layDanhSach() {
        List<KhungGioDTO> ketQua = khungGioRepository.findAll().stream()
                .map(KhungGioDTO::tu)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ketQua);
    }

    @GetMapping("/{id}")
    public ResponseEntity<KhungGioDTO> layChiTiet(@PathVariable Long id) {
        return khungGioRepository.findById(id)
                .map(k -> ResponseEntity.ok(KhungGioDTO.tu(k)))
                .orElse(ResponseEntity.notFound().build());
    }

    // POST - chỉ admin tạo khung giờ mới
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KhungGioDTO> taoMoi(@RequestBody KhungGio khungGio) {
        if (khungGio.getId() != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tạo khung giờ mới không được truyền ID");
        kiemTraKhoangGio(khungGio, null);
        return ResponseEntity.ok(KhungGioDTO.tu(khungGioRepository.save(khungGio)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<KhungGioDTO> capNhat(@PathVariable Long id, @RequestBody KhungGio request) {
        KhungGio khungGio = khungGioRepository.findLockedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khung giờ"));
        if (khungGio.getDanhSachDatSan() != null && !khungGio.getDanhSachDatSan().isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Khung giờ có lịch sử đặt; hãy tạo khung giờ mới");
        kiemTraKhoangGio(request, id);
        khungGio.setGioBatDau(request.getGioBatDau());
        khungGio.setGioKetThuc(request.getGioKetThuc());
        return ResponseEntity.ok(KhungGioDTO.tu(khungGioRepository.save(khungGio)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> xoa(@PathVariable Long id) {
        if (!khungGioRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        KhungGio item = khungGioRepository.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if ((item.getDanhSachDatSan() != null && !item.getDanhSachDatSan().isEmpty()) || (item.getDanhSachGia() != null && !item.getDanhSachGia().isEmpty())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Khung giờ đã được sử dụng; không thể xóa");
        khungGioRepository.delete(item);
        return ResponseEntity.noContent().build();
    }

    private void kiemTraKhoangGio(KhungGio khungGio, Long excludedId) {
        if (khungGio.getGioBatDau() == null || khungGio.getGioKetThuc() == null
                || !khungGio.getGioKetThuc().isAfter(khungGio.getGioBatDau())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giờ kết thúc phải sau giờ bắt đầu");
        }
        boolean overlaps = khungGioRepository.findAll().stream()
                .filter(existing -> !java.util.Objects.equals(existing.getId(), excludedId))
                .anyMatch(existing -> existing.getGioBatDau().isBefore(khungGio.getGioKetThuc())
                        && existing.getGioKetThuc().isAfter(khungGio.getGioBatDau()));
        if (overlaps) throw new ResponseStatusException(HttpStatus.CONFLICT, "Khung giờ giao nhau với khung đã có");
    }
}
