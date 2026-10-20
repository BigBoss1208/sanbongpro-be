package com.example.becicd.service;

import com.example.becicd.entity.*;
import com.example.becicd.enums.*;
import com.example.becicd.repository.*;
import com.example.becicd.security.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;

@Service
public class BookingGuard {
    @javax.persistence.PersistenceContext
    private javax.persistence.EntityManager entityManager;
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final SanRepository fields;
    private final DatSanRepository bookings;
    public BookingGuard(SanRepository fields, DatSanRepository bookings) {
        this.fields = fields; this.bookings = bookings;
    }
    public static LocalDateTime now() { return LocalDateTime.now(ZONE); }
    public static boolean active(San field) { return "HOAT_DONG".equals(field.getTrangThai()); }
    public static boolean occupies(DatSan booking) {
        if (booking.getTrangThai() == TrangThaiDatSan.GIU_CHO_TAM) {
            return booking.getThoiGianHetHanGiuCho() != null && booking.getThoiGianHetHanGiuCho().isAfter(now());
        }
        return booking.getTrangThai() == TrangThaiDatSan.CHO_XAC_NHAN
            || booking.getTrangThai() == TrangThaiDatSan.DA_XAC_NHAN
            || booking.getTrangThai() == TrangThaiDatSan.DA_CHECKIN
            || booking.getTrangThai() == TrangThaiDatSan.HOAN_THANH;
    }
    public static boolean overlaps(KhungGio a, KhungGio b) {
        return a.getGioBatDau().isBefore(b.getGioKetThuc()) && b.getGioBatDau().isBefore(a.getGioKetThuc());
    }
    public San lockField(Long id) {
        return fields.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân"));
    }
    // Every writer obtains the same field row lock before inspecting bookings.
    public DatSan lockBooking(Long id) {
        DatSan initial = bookings.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt sân"));
        lockField(initial.getSan().getId());
        DatSan locked = bookings.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt sân"));
        entityManager.refresh(locked);
        return locked;
    }
    public void validateNew(San field, KhungGio slot, LocalDate date) {
        if (!active(field)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Sân đang tạm ngưng hoạt động");
        if (date == null || slot.getGioBatDau() == null || slot.getGioKetThuc() == null
            || !slot.getGioBatDau().isBefore(slot.getGioKetThuc())
            || !date.atTime(slot.getGioBatDau()).isAfter(now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày hoặc khung giờ không hợp lệ, đã bắt đầu hoặc đã qua");
        }
        if (bookings.findBySanIdAndNgayDat(field.getId(), date).stream().anyMatch(b -> occupies(b) && overlaps(slot, b.getKhungGio()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khoảng giờ này đã có người đặt");
        }
    }
    public static NguoiDung current() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof CustomUserDetails)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return ((CustomUserDetails) principal).getNguoiDung();
    }
    public static boolean mayAccess(San field) {
        NguoiDung user = current();
        return user.getVaiTro() == VaiTro.ADMIN || (user.getCumSan() != null && field.getCumSan() != null
            && user.getCumSan().getId().equals(field.getCumSan().getId()));
    }
    public static void requireStaffScope(San field) {
        if (!mayAccess(field)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sân không thuộc cụm được phân công");
    }
}
