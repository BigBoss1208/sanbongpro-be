package com.example.becicd.controller;

import com.example.becicd.dto.DatSanDTO;
import com.example.becicd.dto.TaoWalkinDatSanRequest;
import com.example.becicd.dto.TuChoiDatSanRequest;
import com.example.becicd.entity.DatSan;
import com.example.becicd.entity.GiaSan;
import com.example.becicd.entity.KhungGio;
import com.example.becicd.entity.NguoiDung;
import com.example.becicd.entity.San;
import com.example.becicd.entity.ThanhToan;
import com.example.becicd.enums.PhuongThucThanhToan;
import com.example.becicd.enums.TrangThaiDatSan;
import com.example.becicd.enums.TrangThaiThanhToan;
import com.example.becicd.enums.VaiTro;
import com.example.becicd.repository.DatSanRepository;
import com.example.becicd.repository.GiaSanRepository;
import com.example.becicd.repository.KhungGioRepository;
import com.example.becicd.repository.NguoiDungRepository;
import com.example.becicd.repository.SanRepository;
import com.example.becicd.repository.ThanhToanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/nhan-vien/dat-san")
public class NhanVienDatSanController {

    private final com.example.becicd.service.BookingGuard guard;
    private final DatSanRepository datSanRepository;
    private final SanRepository sanRepository;
    private final KhungGioRepository khungGioRepository;
    private final GiaSanRepository giaSanRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final PasswordEncoder passwordEncoder;

    public NhanVienDatSanController(
            DatSanRepository datSanRepository,
            SanRepository sanRepository,
            KhungGioRepository khungGioRepository,
            GiaSanRepository giaSanRepository,
            NguoiDungRepository nguoiDungRepository,
            ThanhToanRepository thanhToanRepository,
            PasswordEncoder passwordEncoder, com.example.becicd.service.BookingGuard guard) {
        this.guard = guard;
        this.datSanRepository = datSanRepository;
        this.sanRepository = sanRepository;
        this.khungGioRepository = khungGioRepository;
        this.giaSanRepository = giaSanRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.thanhToanRepository = thanhToanRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_NHAN_VIEN_PAGE')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DatSanDTO>> danhSachDonChoDuyet(
            @RequestParam(defaultValue = "CHO_XAC_NHAN") TrangThaiDatSan trangThai) {
        List<DatSanDTO> result = datSanRepository.findByTrangThaiOrderByNgayTaoDesc(trangThai)
                .stream()
                .filter(item -> com.example.becicd.service.BookingGuard.mayAccess(item.getSan()))
                .map(DatSanDTO::tu)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/duyet")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_NHAN_VIEN_PAGE')")
    @Transactional
    public ResponseEntity<DatSanDTO> duyetDon(@PathVariable Long id) {
        DatSan datSan = timDon(id);
        if (datSan.getTrangThai() != TrangThaiDatSan.CHO_XAC_NHAN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn không còn ở trạng thái chờ xác nhận");
        }
        datSan.setTrangThai(TrangThaiDatSan.DA_XAC_NHAN);
        datSan.setLyDoTuChoi(null);
        return ResponseEntity.ok(DatSanDTO.tu(datSanRepository.save(datSan)));
    }

    @PutMapping("/{id}/tu-choi")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_NHAN_VIEN_PAGE')")
    @Transactional
    public ResponseEntity<DatSanDTO> tuChoiDon(
            @PathVariable Long id,
            @Valid @RequestBody TuChoiDatSanRequest request) {
        DatSan datSan = timDon(id);
        if (datSan.getTrangThai() != TrangThaiDatSan.CHO_XAC_NHAN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn không còn ở trạng thái chờ xác nhận");
        }
        datSan.setTrangThai(TrangThaiDatSan.DA_HUY);
        datSan.setLyDoTuChoi(request.getLyDo().trim());
        for (ThanhToan payment : thanhToanRepository.findByDatSanIdOrderByIdDesc(id)) {
            if (payment.getTrangThai() == TrangThaiThanhToan.CHO_THANH_TOAN) payment.setTrangThai(TrangThaiThanhToan.THAT_BAI);
            if (payment.getTrangThai() == TrangThaiThanhToan.THANH_CONG) payment.setCanXuLyHoanTien(true);
        }
        return ResponseEntity.ok(DatSanDTO.tu(datSanRepository.save(datSan)));
    }

    @GetMapping("/tim-kiem")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_NHAN_VIEN_SAN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DatSanDTO>> timDon(@RequestParam String q) {
        String query = q == null ? "" : q.trim();
        if (query.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập mã đơn hoặc số điện thoại");
        }

        Map<Long, DatSan> matches = new LinkedHashMap<>();
        if (query.matches("\\d+")) {
            try {
                datSanRepository.findById(Long.parseLong(query)).ifPresent(item -> matches.put(item.getId(), item));
            } catch (NumberFormatException ignored) {
                // The query may be a phone number longer than a booking ID.
            }
        }

        String phoneDigits = query.replaceAll("\\D", "");
        if (!phoneDigits.isEmpty()) {
            datSanRepository.findByKhachHangSoDienThoaiContainingOrderByNgayDatDesc(phoneDigits)
                    .forEach(item -> matches.putIfAbsent(item.getId(), item));
        }

        List<DatSanDTO> result = matches.values().stream().filter(item -> com.example.becicd.service.BookingGuard.mayAccess(item.getSan()))
                .sorted(Comparator.comparing(DatSan::getNgayTao).reversed())
                .map(DatSanDTO::tu)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/checkin")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_NHAN_VIEN_SAN')")
    @Transactional
    public ResponseEntity<DatSanDTO> checkIn(@PathVariable Long id) {
        DatSan datSan = timDon(id);
        if (datSan.getTrangThai() == TrangThaiDatSan.DA_CHECKIN) {
            return ResponseEntity.ok(DatSanDTO.tu(datSan));
        }
        if (datSan.getTrangThai() != TrangThaiDatSan.DA_XAC_NHAN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ check-in được đơn đã xác nhận");
        }
        LocalDateTime start = datSan.getNgayDat().atTime(datSan.getKhungGio().getGioBatDau());
        LocalDateTime end = datSan.getNgayDat().atTime(datSan.getKhungGio().getGioKetThuc());
        LocalDateTime now = com.example.becicd.service.BookingGuard.now();
        if (now.isBefore(start.minusMinutes(30)) || now.isAfter(end)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ check-in từ 30 phút trước giờ bắt đầu đến giờ kết thúc");
        datSan.setTrangThai(TrangThaiDatSan.DA_CHECKIN);
        return ResponseEntity.ok(DatSanDTO.tu(datSanRepository.save(datSan)));
    }

    @PostMapping("/walkin")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_NHAN_VIEN_SAN')")
    @Transactional
    public ResponseEntity<DatSanDTO> taoDonTaiQuay(@Valid @RequestBody TaoWalkinDatSanRequest request) {
        LocalDate ngayDat = request.getNgayDat();
        if (ngayDat.isBefore(com.example.becicd.service.BookingGuard.now().toLocalDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể đặt sân cho ngày đã qua");
        }

        San san = sanRepository.findLockedById(request.getSanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân"));
        KhungGio khungGio = khungGioRepository.findLockedById(request.getKhungGioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khung giờ"));
        if (ngayDat.equals(com.example.becicd.service.BookingGuard.now().toLocalDate()) && khungGio.getGioBatDau().isBefore(com.example.becicd.service.BookingGuard.now().toLocalTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khung giờ đã bắt đầu hoặc đã qua");
        }

        com.example.becicd.service.BookingGuard.requireStaffScope(san);
        guard.validateNew(san, khungGio, ngayDat);
        if (request.getPhuongThucThanhToan() == null || request.getPhuongThucThanhToan() == PhuongThucThanhToan.VNPAY) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tại quầy chỉ hỗ trợ tiền mặt hoặc chuyển khoản");
        boolean slotBusy = datSanRepository.findBySanIdAndKhungGioIdAndNgayDat(
                        san.getId(), khungGio.getId(), ngayDat)
                .stream()
                .anyMatch(this::dangChiếmCho);
        if (slotBusy) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khung giờ này vừa được đặt, vui lòng chọn giờ khác");
        }

        GiaSan giaSan = timGiaChoNgay(san.getId(), khungGio.getId(), ngayDat)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sân chưa được cấu hình giá cho khung giờ này"));
        NguoiDung khachHang = timHoacTaoKhachWalkin(
                request.getTenKhachHang().trim(), request.getSoDienThoai().trim());

        DatSan datSan = new DatSan();
        datSan.setKhachHang(khachHang);
        datSan.setSan(san);
        datSan.setKhungGio(khungGio);
        datSan.setNgayDat(ngayDat);
        datSan.setGia(giaSan.getGia());
        datSan.setTrangThai(TrangThaiDatSan.DA_XAC_NHAN);
        datSan.setThoiGianHetHanGiuCho(null);
        DatSan saved = datSanRepository.save(datSan);

        PhuongThucThanhToan method = request.getPhuongThucThanhToan() == null
                ? PhuongThucThanhToan.TIEN_MAT : request.getPhuongThucThanhToan();
        ThanhToan payment = new ThanhToan();
        payment.setDatSan(saved);
        payment.setSoTien(saved.getGia());
        payment.setTongTien(saved.getGia());
        payment.setPhuongThuc(method);
        payment.setTrangThai(method == PhuongThucThanhToan.TIEN_MAT
                ? TrangThaiThanhToan.THANH_CONG : TrangThaiThanhToan.CHO_THANH_TOAN);
        if (method == PhuongThucThanhToan.TIEN_MAT) {
            payment.setThoiGianThanhToan(com.example.becicd.service.BookingGuard.now());
        }
        thanhToanRepository.save(payment);

        return ResponseEntity.status(HttpStatus.CREATED).body(DatSanDTO.tu(saved));
    }

    private DatSan timDon(Long id) {
        DatSan booking = guard.lockBooking(id);
        com.example.becicd.service.BookingGuard.requireStaffScope(booking.getSan());
        return booking;
    }

    private boolean dangChiếmCho(DatSan datSan) {
        if (datSan.getTrangThai() == TrangThaiDatSan.GIU_CHO_TAM) {
            return datSan.getThoiGianHetHanGiuCho() != null
                    && datSan.getThoiGianHetHanGiuCho().isAfter(com.example.becicd.service.BookingGuard.now());
        }
        return datSan.getTrangThai() == TrangThaiDatSan.CHO_XAC_NHAN
                || datSan.getTrangThai() == TrangThaiDatSan.DA_XAC_NHAN
                || datSan.getTrangThai() == TrangThaiDatSan.DA_CHECKIN
                || datSan.getTrangThai() == TrangThaiDatSan.HOAN_THANH;
    }

    private Optional<GiaSan> timGiaChoNgay(Long sanId, Long khungGioId, LocalDate ngay) {
        String loaiNgay = ngay.getDayOfWeek() == DayOfWeek.SATURDAY || ngay.getDayOfWeek() == DayOfWeek.SUNDAY
                ? "CUOI_TUAN" : "NGAY_THUONG";
        List<GiaSan> prices = giaSanRepository.findAllBySanIdAndKhungGioId(sanId, khungGioId);
        Optional<GiaSan> specificPrice = prices.stream()
                .filter(price -> loaiNgay.equalsIgnoreCase(price.getLoai_ngay()))
                .findFirst();
        if (specificPrice.isPresent()) return specificPrice;
        return prices.stream()
                .filter(price -> price.getLoai_ngay() == null || "TAT_CA".equalsIgnoreCase(price.getLoai_ngay()))
                .findFirst();
    }

    private NguoiDung timHoacTaoKhachWalkin(String tenKhachHang, String soDienThoai) {
        Optional<NguoiDung> existing = nguoiDungRepository
                .findFirstBySoDienThoaiAndVaiTroOrderByIdAsc(soDienThoai, VaiTro.KHACH_HANG);
        if (existing.isPresent()) return existing.get();

        String suffix = UUID.randomUUID().toString().replace("-", "");
        NguoiDung walkin = NguoiDung.builder()
                .username("walkin-" + suffix)
                .email("walkin+" + suffix + "@walkin.invalid")
                .matKhau(passwordEncoder.encode(UUID.randomUUID().toString()))
                .hoTen(tenKhachHang)
                .soDienThoai(soDienThoai)
                .vaiTro(VaiTro.KHACH_HANG)
                .trangThai(true)
                .build();
        return nguoiDungRepository.save(walkin);
    }
}
