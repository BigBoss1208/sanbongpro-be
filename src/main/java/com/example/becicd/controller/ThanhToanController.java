package com.example.becicd.controller;

import com.example.becicd.entity.DatSan;
import com.example.becicd.entity.ThanhToan;
import com.example.becicd.enums.PhuongThucThanhToan;
import com.example.becicd.enums.TrangThaiThanhToan;
import com.example.becicd.repository.DatSanRepository;
import com.example.becicd.repository.ThanhToanRepository;
import com.example.becicd.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/thanh-toan")
public class ThanhToanController {

    @javax.persistence.PersistenceContext private javax.persistence.EntityManager entityManager;
    private final com.example.becicd.service.BookingGuard guard;
    private final ThanhToanRepository thanhToanRepository;
    private final DatSanRepository datSanRepository;

    public ThanhToanController(ThanhToanRepository thanhToanRepository, DatSanRepository datSanRepository, com.example.becicd.service.BookingGuard guard) {
        this.guard = guard;
        this.thanhToanRepository = thanhToanRepository;
        this.datSanRepository = datSanRepository;
    }

    @GetMapping("/quan-ly")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN_SAN')")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> danhSachGiaoDich(@RequestParam(required = false) TrangThaiThanhToan trangThai) {
        List<ThanhToan> records = trangThai == null
                ? thanhToanRepository.findAll()
                : thanhToanRepository.findByTrangThaiOrderByIdDesc(trangThai);
        return records.stream().filter(p -> com.example.becicd.service.BookingGuard.mayAccess(p.getDatSan().getSan())).map(this::toMap).collect(Collectors.toList());
    }

    @GetMapping("/dat-san/{datSanId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> giaoDichCuaDon(
            @PathVariable Long datSanId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        DatSan datSan = timDon(datSanId);
        kiemTraQuyenXem(datSan, currentUser);
        return thanhToanRepository.findByDatSanIdOrderByIdDesc(datSanId)
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    @GetMapping("/dat-san/{datSanId}/hoa-don")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Map<String, Object> hoaDon(
            @PathVariable Long datSanId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        DatSan booking = timDon(datSanId);
        kiemTraQuyenXem(booking, currentUser);
        Map<String, Object> invoice = new LinkedHashMap<>();
        invoice.put("maHoaDon", "HD-" + booking.getId());
        invoice.put("datSanId", booking.getId());
        invoice.put("tenKhachHang", booking.getKhachHang().getHoTen());
        invoice.put("soDienThoai", booking.getKhachHang().getSoDienThoai());
        invoice.put("tenSan", booking.getSan().getTenSan());
        invoice.put("ngayDat", booking.getNgayDat());
        invoice.put("gioBatDau", booking.getKhungGio().getGioBatDau());
        invoice.put("gioKetThuc", booking.getKhungGio().getGioKetThuc());
        invoice.put("gia", booking.getGia());
        invoice.put("trangThaiDatSan", booking.getTrangThai().name());
        invoice.put("giaoDich", thanhToanRepository.findByDatSanIdOrderByIdDesc(datSanId)
                .stream().map(this::toMap).collect(Collectors.toList()));
        return invoice;
    }

    @PutMapping("/{id}/xac-nhan")
    @PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN_SAN')")
    @Transactional
    public Map<String, Object> xacNhanThanhToan(@PathVariable Long id) {
        ThanhToan payment = thanhToanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch"));
        DatSan booking = guard.lockBooking(payment.getDatSan().getId());
        entityManager.refresh(payment);
        com.example.becicd.service.BookingGuard.requireStaffScope(booking.getSan());
        if (booking.getTrangThai() != com.example.becicd.enums.TrangThaiDatSan.DA_XAC_NHAN
                && booking.getTrangThai() != com.example.becicd.enums.TrangThaiDatSan.DA_CHECKIN
                && booking.getTrangThai() != com.example.becicd.enums.TrangThaiDatSan.HOAN_THANH)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn chưa được xác nhận hoặc đã hủy");
        if (payment.getTrangThai() == TrangThaiThanhToan.THANH_CONG) return toMap(payment);
        if (payment.getTrangThai() != TrangThaiThanhToan.CHO_THANH_TOAN
                || payment.getPhuongThuc() == PhuongThucThanhToan.VNPAY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ xác nhận giao dịch tiền mặt/chuyển khoản đang chờ");
        }
        payment.setTrangThai(TrangThaiThanhToan.THANH_CONG);
        payment.setThoiGianThanhToan(com.example.becicd.service.BookingGuard.now());
        return toMap(thanhToanRepository.save(payment));
    }

    private DatSan timDon(Long id) {
        return datSanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt sân"));
    }

    private void kiemTraQuyenXem(DatSan booking, CustomUserDetails currentUser) {
        boolean staff = currentUser.getAuthorities().stream().anyMatch(authority ->
                "ROLE_ADMIN".equals(authority.getAuthority()) || "ROLE_NHAN_VIEN_SAN".equals(authority.getAuthority()));
        if (staff) com.example.becicd.service.BookingGuard.requireStaffScope(booking.getSan());
        if (!staff && !booking.getKhachHang().getId().equals(currentUser.getNguoiDung().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem hóa đơn này");
        }
    }

    private Map<String, Object> toMap(ThanhToan payment) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", payment.getId());
        result.put("datSanId", payment.getDatSan().getId());
        result.put("tenKhachHang", payment.getDatSan().getKhachHang().getHoTen());
        result.put("soDienThoai", payment.getDatSan().getKhachHang().getSoDienThoai());
        result.put("tenSan", payment.getDatSan().getSan().getTenSan());
        result.put("ngayDat", payment.getDatSan().getNgayDat());
        result.put("soTien", payment.getSoTien());
        result.put("tongTien", payment.getTongTien());
        result.put("canXuLyHoanTien", payment.isCanXuLyHoanTien());
        result.put("phuongThuc", payment.getPhuongThuc().name());
        result.put("trangThai", payment.getTrangThai().name());
        result.put("maGiaoDich", payment.getMaGiaoDichVnpay());
        result.put("thoiGianThanhToan", payment.getThoiGianThanhToan());
        return result;
    }
}
