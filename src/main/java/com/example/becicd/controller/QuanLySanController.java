package com.example.becicd.controller;

import com.example.becicd.dto.MatrixLichSanDTO;
import com.example.becicd.entity.DatSan;
import com.example.becicd.entity.GiaSan;
import com.example.becicd.entity.San;
import com.example.becicd.enums.TrangThaiDatSan;
import com.example.becicd.repository.DatSanRepository;
import com.example.becicd.repository.SanRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/quan-ly", "/api/nhan-vien"})
public class QuanLySanController {

    private final SanRepository sanRepository;
    private final DatSanRepository datSanRepository;

    public QuanLySanController(SanRepository sanRepository, DatSanRepository datSanRepository) {
        this.sanRepository = sanRepository;
        this.datSanRepository = datSanRepository;
    }

    // API Ma trận lịch trống toàn bộ sân theo ngày
    @GetMapping({"/ma-tran-lich", "/lich-ma-tran"})
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_NHAN_VIEN_PAGE', 'ROLE_NHAN_VIEN_SAN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MatrixLichSanDTO>> layMaTranLich(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {

        List<San> dsSan = sanRepository.findAll().stream().filter(com.example.becicd.service.BookingGuard::mayAccess).collect(Collectors.toList());
        boolean laCuoiTuan = (ngay.getDayOfWeek() == DayOfWeek.SATURDAY || ngay.getDayOfWeek() == DayOfWeek.SUNDAY);
        String loaiNgayTimKiem = laCuoiTuan ? "CUOI_TUAN" : "NGAY_THUONG";

        List<MatrixLichSanDTO> ketQua = new ArrayList<>();

        for (San san : dsSan) {
            List<DatSan> dsDaDat = datSanRepository.findBySanIdAndNgayDat(san.getId(), ngay);

            List<MatrixLichSanDTO.SlotItemDTO> slots = new ArrayList<>();
            Map<Long, GiaSan> giaTheoKhungGio = new LinkedHashMap<>();

            if (san.getDanhSachGia() != null) {
                for (GiaSan giaSan : san.getDanhSachGia()) {
                    String loaiNgayGia = giaSan.getLoai_ngay();
                    boolean giaCuThe = loaiNgayTimKiem.equalsIgnoreCase(loaiNgayGia == null ? "" : loaiNgayGia);
                    boolean giaTatCa = loaiNgayGia == null || "TAT_CA".equalsIgnoreCase(loaiNgayGia);
                    if (!giaCuThe && !giaTatCa) continue;

                    Long khungGioId = giaSan.getKhungGio().getId();
                    GiaSan giaHienTai = giaTheoKhungGio.get(khungGioId);
                    if (giaHienTai == null || (!laGiaCuThe(giaHienTai, loaiNgayTimKiem) && giaCuThe)) {
                        giaTheoKhungGio.put(khungGioId, giaSan);
                    }
                }
            }

            for (GiaSan giaSan : giaTheoKhungGio.values()) {
                Long khungGioId = giaSan.getKhungGio().getId();
                DatSan datSan = dsDaDat.stream().filter(com.example.becicd.service.BookingGuard::occupies)
                        .filter(d -> com.example.becicd.service.BookingGuard.overlaps(d.getKhungGio(), giaSan.getKhungGio()))
                        .findFirst().orElse(null);
                String trangThai = com.example.becicd.service.BookingGuard.active(san)
                        && ngay.atTime(giaSan.getKhungGio().getGioBatDau()).isAfter(com.example.becicd.service.BookingGuard.now()) ? "TRONG" : "KHONG_HOAT_DONG";
                String tenKhach = "";
                String sdt = "";
                Long datSanId = null;

                if (datSan != null) {
                    TrangThaiDatSan status = datSan.getTrangThai();
                    trangThai = status == TrangThaiDatSan.DA_XAC_NHAN
                            || status == TrangThaiDatSan.DA_CHECKIN
                            || status == TrangThaiDatSan.HOAN_THANH
                            ? "DA_DAT" : status.name();
                    tenKhach = datSan.getKhachHang() != null ? datSan.getKhachHang().getHoTen() : "Khách vãng lai";
                    sdt = datSan.getKhachHang() != null ? datSan.getKhachHang().getSoDienThoai() : "";
                    datSanId = datSan.getId();
                }

                slots.add(new MatrixLichSanDTO.SlotItemDTO(
                        khungGioId,
                        giaSan.getKhungGio().getGioBatDau().toString(),
                        giaSan.getKhungGio().getGioKetThuc().toString(),
                        datSan == null ? giaSan.getGia() : datSan.getGia(),
                        trangThai,
                        tenKhach,
                        sdt,
                        datSanId
                ));
            }

            ketQua.add(new MatrixLichSanDTO(san.getId(), san.getTenSan(), san.getLoaiSan(), slots));
        }

        return ResponseEntity.ok(ketQua);
    }

    private boolean laGiaCuThe(GiaSan giaSan, String loaiNgay) {
        return giaSan.getLoai_ngay() != null && loaiNgay.equalsIgnoreCase(giaSan.getLoai_ngay());
    }

}
