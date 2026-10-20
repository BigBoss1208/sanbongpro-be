package com.example.becicd.controller;

import com.example.becicd.dto.DatSanDTO;
import com.example.becicd.dto.TaoDatSanRequest;
import com.example.becicd.entity.DatSan;
import com.example.becicd.entity.GiaSan;
import com.example.becicd.entity.KhungGio;
import com.example.becicd.entity.NguoiDung;
import com.example.becicd.entity.San;
import com.example.becicd.entity.ThanhToan;
import com.example.becicd.enums.PhuongThucThanhToan;
import com.example.becicd.enums.TrangThaiDatSan;
import com.example.becicd.enums.TrangThaiThanhToan;
import com.example.becicd.repository.DatSanRepository;
import com.example.becicd.repository.GiaSanRepository;
import com.example.becicd.repository.KhungGioRepository;
import com.example.becicd.repository.SanRepository;
import com.example.becicd.repository.ThanhToanRepository;
import com.example.becicd.security.CustomUserDetails;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dat-san")
public class DatSanController {

    private static final int THOI_GIAN_GIU_CHO_PHUT = 15;

    private static final List<TrangThaiDatSan> TRANG_THAI_CHIEM_CHO = List.of(
            TrangThaiDatSan.GIU_CHO_TAM,
            TrangThaiDatSan.CHO_XAC_NHAN,
            TrangThaiDatSan.DA_XAC_NHAN,
            TrangThaiDatSan.DA_CHECKIN
    );

    private final com.example.becicd.service.BookingGuard guard;
    private final DatSanRepository datSanRepository;
    private final SanRepository sanRepository;
    private final KhungGioRepository khungGioRepository;
    private final GiaSanRepository giaSanRepository;
    private final ThanhToanRepository thanhToanRepository;

    public DatSanController(
            DatSanRepository datSanRepository,
            SanRepository sanRepository,
            KhungGioRepository khungGioRepository,
            GiaSanRepository giaSanRepository,
            ThanhToanRepository thanhToanRepository, com.example.becicd.service.BookingGuard guard
    ) {
        this.guard = guard;
        this.datSanRepository = datSanRepository;
        this.sanRepository = sanRepository;
        this.khungGioRepository = khungGioRepository;
        this.giaSanRepository = giaSanRepository;
        this.thanhToanRepository = thanhToanRepository;
    }

    /**
     * Lấy người dùng hiện tại từ JWT
     */
    private NguoiDung nguoiDungHienTai() {
        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        return userDetails.getNguoiDung();
    }

    /**
     * Tạo đơn đặt sân
     */
    @PostMapping
    @PreAuthorize("hasRole('KHACH_HANG')")
    @Transactional
    public ResponseEntity<DatSanDTO> taoDatSan(
            @Valid @RequestBody TaoDatSanRequest request
    ) {

        // =========================================================
        // 1. Kiểm tra sân
        // =========================================================
        San san = sanRepository.findLockedById(request.getSanId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy sân"
                        )
                );

        // =========================================================
        // 2. Kiểm tra khung giờ
        // =========================================================
        KhungGio khungGio = khungGioRepository.findLockedById(request.getKhungGioId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy khung giờ"
                        )
                );

        // =========================================================
        guard.validateNew(san, khungGio, request.getNgayDat());
        if (request.getPhuongThucThanhToan() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn phương thức thanh toán");

        // 3. Xác định ngày thường / cuối tuần
        // =========================================================
        boolean laCuoiTuan =
                request.getNgayDat().getDayOfWeek() == DayOfWeek.SATURDAY
                        || request.getNgayDat().getDayOfWeek() == DayOfWeek.SUNDAY;

        String loaiNgay = laCuoiTuan
                ? "CUOI_TUAN"
                : "NGAY_THUONG";

        // =========================================================
        // 4. Lấy giá sân
        // =========================================================
        List<GiaSan> giaTheoKhungGio =
                giaSanRepository.findAllBySanIdAndKhungGioId(
                        san.getId(),
                        khungGio.getId()
                );

        GiaSan giaSan = giaTheoKhungGio.stream()
                .filter(g ->
                        loaiNgay.equalsIgnoreCase(g.getLoai_ngay())
                )
                .findFirst()
                .orElseGet(() ->
                        giaTheoKhungGio.stream()
                                .filter(g ->
                                        g.getLoai_ngay() == null
                                                || "TAT_CA".equalsIgnoreCase(
                                                g.getLoai_ngay()
                                        )
                                )
                                .findFirst()
                                .orElse(null)
                );

        if (giaSan == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sân này chưa được cấu hình giá cho khung giờ đã chọn"
            );
        }

        // =========================================================
        // 5. Kiểm tra khung giờ đã có người đặt chưa
        // =========================================================
        boolean daBiChiem =
                datSanRepository
                        .findBySanIdAndKhungGioIdAndNgayDat(
                                san.getId(),
                                khungGio.getId(),
                                request.getNgayDat()
                        )
                        .stream()
                        .anyMatch(this::conHieuLuc);

        if (daBiChiem) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Khung giờ này đã có người đặt"
            );
        }

        // =========================================================
        // 6. Tạo đơn đặt sân
        // =========================================================
        DatSan datSan = new DatSan();

        datSan.setKhachHang(nguoiDungHienTai());
        datSan.setSan(san);
        datSan.setKhungGio(khungGio);
        datSan.setNgayDat(request.getNgayDat());

        // Giá sân
        datSan.setGia(giaSan.getGia());

        // =========================================================
        // 7. Xác định phương thức thanh toán
        // =========================================================
        boolean thanhToanTaiSan =
                request.getPhuongThucThanhToan() == PhuongThucThanhToan.TIEN_MAT
                        || request.getPhuongThucThanhToan() == PhuongThucThanhToan.CHUYEN_KHOAN;

        // =========================================================
        // 8. Xác định trạng thái đặt sân
        // =========================================================
        datSan.setTrangThai(
                thanhToanTaiSan
                        ? TrangThaiDatSan.CHO_XAC_NHAN
                        : TrangThaiDatSan.GIU_CHO_TAM
        );

        // =========================================================
        // 9. Thời gian giữ chỗ
        // =========================================================
        datSan.setThoiGianHetHanGiuCho(
                thanhToanTaiSan
                        ? null
                        : com.example.becicd.service.BookingGuard.now()
                          .plusMinutes(THOI_GIAN_GIU_CHO_PHUT)
        );

        // =========================================================
        // 10. Lưu đơn đặt sân
        // =========================================================
        DatSan daLuu = datSanRepository.save(datSan);

        // =========================================================
        // 11. Tạo thanh toán
        // =========================================================
        if (thanhToanTaiSan) {

            ThanhToan payment = new ThanhToan();

            payment.setDatSan(daLuu);

            // Cột so_tien
            payment.setSoTien(daLuu.getGia());

            // Cột tong_tien
            payment.setTongTien(daLuu.getGia());

            // Phương thức thanh toán
            payment.setPhuongThuc(
                    request.getPhuongThucThanhToan()
            );

            // Trạng thái thanh toán
            payment.setTrangThai(
                    TrangThaiThanhToan.CHO_THANH_TOAN
            );

            thanhToanRepository.save(payment);
        }

        // =========================================================
        // 12. Trả kết quả
        // =========================================================
        return new ResponseEntity<>(
                DatSanDTO.tu(daLuu),
                HttpStatus.CREATED
        );
    }

    /**
     * Lấy danh sách đơn đặt sân của khách hàng hiện tại
     */
    @GetMapping("/cua-toi")
    @PreAuthorize("hasRole('KHACH_HANG')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<DatSanDTO>> datSanCuaToi() {

        List<DatSanDTO> ketQua =
                datSanRepository
                        .findByKhachHangIdOrderByNgayTaoDesc(
                                nguoiDungHienTai().getId()
                        )
                        .stream()
                        .map(booking -> {
                            DatSanDTO dto = DatSanDTO.tu(booking);
                            dto.setPayment(thanhToanRepository.findByDatSanIdOrderByIdDesc(booking.getId()).stream().findFirst().orElse(null));
                            return dto;
                        })
                        .collect(Collectors.toList());

        return ResponseEntity.ok(ketQua);
    }

    /**
     * Lấy danh sách các đơn đang giữ chỗ
     */
    @GetMapping("/giu-cho/cua-toi")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<List<DatSanDTO>> giuChoCuaToi() {

        LocalDateTime now = com.example.becicd.service.BookingGuard.now();

        List<DatSanDTO> ketQua =
                datSanRepository
                        .findByKhachHangIdOrderByNgayTaoDesc(
                                nguoiDungHienTai().getId()
                        )
                        .stream()
                        .filter(d ->
                                d.getTrangThai()
                                        == TrangThaiDatSan.GIU_CHO_TAM
                                        && d.getThoiGianHetHanGiuCho() != null
                                        && d.getThoiGianHetHanGiuCho()
                                        .isAfter(now)
                        )
                        .map(DatSanDTO::tu)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(ketQua);
    }

    /**
     * Xem chi tiết một đơn đặt sân
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('KHACH_HANG')")
    public ResponseEntity<DatSanDTO> chiTietCuaToi(
            @PathVariable Long id
    ) {

        DatSan booking =
                datSanRepository.findById(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Không tìm thấy đơn đặt sân"
                                )
                        );

        if (!booking.getKhachHang()
                .getId()
                .equals(nguoiDungHienTai().getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền xem đơn này"
            );
        }

        return ResponseEntity.ok(
                DatSanDTO.tu(booking)
        );
    }

    /**
     * Hủy đơn đặt sân
     */
    @PutMapping("/{id}/huy")
    @PreAuthorize("hasRole('KHACH_HANG')")
    @Transactional
    public ResponseEntity<DatSanDTO> huyDatSan(
            @PathVariable Long id
    ) {

        DatSan datSan = guard.lockBooking(id);

        // Kiểm tra quyền sở hữu
        if (!datSan.getKhachHang()
                .getId()
                .equals(nguoiDungHienTai().getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền hủy đơn này"
            );
        }

        // Kiểm tra trạng thái
        if (datSan.getTrangThai() == TrangThaiDatSan.DA_HUY
                || datSan.getTrangThai() == TrangThaiDatSan.DA_CHECKIN
                || datSan.getTrangThai() == TrangThaiDatSan.HOAN_THANH) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn đặt sân không thể hủy ở trạng thái hiện tại"
            );
        }

        // Kiểm tra thời gian
        if (!datSan.getNgayDat().atTime(datSan.getKhungGio().getGioBatDau()).isAfter(com.example.becicd.service.BookingGuard.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hủy đơn sau khi khung giờ đã bắt đầu");
        }
        for (ThanhToan payment : thanhToanRepository.findByDatSanIdOrderByIdDesc(id)) {
            if (payment.getTrangThai() == TrangThaiThanhToan.CHO_THANH_TOAN) payment.setTrangThai(TrangThaiThanhToan.THAT_BAI);
            if (payment.getTrangThai() == TrangThaiThanhToan.THANH_CONG) payment.setCanXuLyHoanTien(true);
        }

        // Cập nhật trạng thái
        datSan.setTrangThai(
                TrangThaiDatSan.DA_HUY
        );

        return ResponseEntity.ok(
                DatSanDTO.tu(
                        datSanRepository.save(datSan)
                )
        );
    }

    /**
     * Kiểm tra đơn còn hiệu lực hay không
     */
    private boolean conHieuLuc(DatSan d) {

        if (!TRANG_THAI_CHIEM_CHO.contains(
                d.getTrangThai()
        )) {
            return false;
        }

        if (d.getTrangThai()
                == TrangThaiDatSan.GIU_CHO_TAM) {

            return d.getThoiGianHetHanGiuCho() != null
                    && d.getThoiGianHetHanGiuCho()
                    .isAfter(com.example.becicd.service.BookingGuard.now());
        }

        return true;
    }
}
