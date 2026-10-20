package com.example.becicd.controller;

import com.example.becicd.dto.LichTrongDTO;
import com.example.becicd.dto.SanDTO;
import com.example.becicd.entity.DatSan;
import com.example.becicd.entity.GiaSan;
import com.example.becicd.entity.San;
import com.example.becicd.enums.TrangThaiDatSan;
import com.example.becicd.repository.DatSanRepository;
import com.example.becicd.repository.SanRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/san")
public class SanPublicController {

    private final SanRepository sanRepository;
    private final DatSanRepository datSanRepository;

    public SanPublicController(SanRepository sanRepository, DatSanRepository datSanRepository) {
        this.sanRepository = sanRepository;
        this.datSanRepository = datSanRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<SanDTO>> danhSachSan() {
        List<SanDTO> ketQua = sanRepository.findAll().stream()
                .filter(com.example.becicd.service.BookingGuard::active)
                .map(SanDTO::tu)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ketQua);
    }

    @GetMapping("/{sanId}/lich-trong")
    @Transactional(readOnly = true)
    public ResponseEntity<List<LichTrongDTO>> xemLichTrong(
            @PathVariable Long sanId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {

        San san = sanRepository.findById(sanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân"));

        if (!com.example.becicd.service.BookingGuard.active(san)) return ResponseEntity.ok(new ArrayList<>());
        List<DatSan> dsDaDat = datSanRepository.findBySanIdAndNgayDat(sanId, ngay).stream()
                .filter(com.example.becicd.service.BookingGuard::occupies).collect(Collectors.toList());

        if (san.getDanhSachGia() == null || san.getDanhSachGia().isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        // Tự động phân biệt Ngày thường / Cuối tuần
        boolean laCuoiTuan = (ngay.getDayOfWeek() == DayOfWeek.SATURDAY || ngay.getDayOfWeek() == DayOfWeek.SUNDAY);
        String loaiNgayTimKiem = laCuoiTuan ? "CUOI_TUAN" : "NGAY_THUONG";

        Map<Long, GiaSan> giaTheoKhungGio = new LinkedHashMap<>();
        for (GiaSan giaSan : san.getDanhSachGia()) {
            String loaiNgay = giaSan.getLoai_ngay();
            boolean giaMacDinh = loaiNgay == null || "TAT_CA".equalsIgnoreCase(loaiNgay);
            boolean giaTheoNgay = loaiNgayTimKiem.equalsIgnoreCase(loaiNgay);
            if (giaMacDinh || giaTheoNgay) {
                GiaSan giaHienTai = giaTheoKhungGio.get(giaSan.getKhungGio().getId());
                if (giaHienTai == null || giaTheoNgay) {
                    giaTheoKhungGio.put(giaSan.getKhungGio().getId(), giaSan);
                }
            }
        }

        List<LichTrongDTO> ketQua = giaTheoKhungGio.values().stream()
                .sorted(Comparator.comparing(giaSan -> giaSan.getKhungGio().getGioBatDau()))
                .map(giaSan -> new LichTrongDTO(
                        giaSan.getKhungGio().getId(),
                        giaSan.getKhungGio().getGioBatDau(),
                        giaSan.getKhungGio().getGioKetThuc(),
                        giaSan.getGia(),
                        dsDaDat.stream().anyMatch(d -> com.example.becicd.service.BookingGuard.overlaps(d.getKhungGio(), giaSan.getKhungGio())) ? "DA_DAT"
                            : ngay.atTime(giaSan.getKhungGio().getGioBatDau()).isAfter(com.example.becicd.service.BookingGuard.now()) ? "TRONG" : "KHONG_HOAT_DONG"
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ketQua);
    }
}
