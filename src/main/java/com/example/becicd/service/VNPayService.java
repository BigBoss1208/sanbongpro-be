package com.example.becicd.service;

import com.example.becicd.config.VNPayConfig;
import com.example.becicd.entity.DatSan;
import com.example.becicd.entity.ThanhToan;
import com.example.becicd.enums.PhuongThucThanhToan;
import com.example.becicd.enums.TrangThaiDatSan;
import com.example.becicd.enums.TrangThaiThanhToan;
import com.example.becicd.repository.DatSanRepository;
import com.example.becicd.repository.ThanhToanRepository;
import com.example.becicd.util.VNPayUtil;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class VNPayService {

    private final org.springframework.context.ApplicationEventPublisher events;
    private final BookingGuard guard;
    @javax.persistence.PersistenceContext private javax.persistence.EntityManager entityManager;
    private final VNPayConfig vnPayConfig;
    private final DatSanRepository datSanRepository;
    private final ThanhToanRepository thanhToanRepository;

    public VNPayService(VNPayConfig vnPayConfig, DatSanRepository datSanRepository,
                        ThanhToanRepository thanhToanRepository, BookingGuard guard, org.springframework.context.ApplicationEventPublisher events) {
        this.events = events;
        this.guard = guard;
        this.vnPayConfig = vnPayConfig;
        this.datSanRepository = datSanRepository;
        this.thanhToanRepository = thanhToanRepository;
    }

    @org.springframework.transaction.annotation.Transactional
    public String createPaymentUrl(Long datSanId, HttpServletRequest request) {
        DatSan datSan = guard.lockBooking(datSanId);
        if (!datSan.getKhachHang().getId().equals(BookingGuard.current().getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Bạn không có quyền thanh toán đơn này");
        if (datSan.getTrangThai() != TrangThaiDatSan.GIU_CHO_TAM || !BookingGuard.occupies(datSan))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Đơn không còn trong thời hạn thanh toán VNPay");
        BigDecimal soTien = datSan.getGia();
        if (soTien == null || soTien.signum() <= 0) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Giá thanh toán không hợp lệ");
        ThanhToan thanhToan = thanhToanRepository.findByDatSanIdOrderByIdDesc(datSanId).stream()
            .filter(payment -> payment.getPhuongThuc() == PhuongThucThanhToan.VNPAY
                && payment.getTrangThai() == TrangThaiThanhToan.CHO_THANH_TOAN)
            .findFirst().orElseGet(ThanhToan::new);
        if (thanhToan.getMaGiaoDichVnpay() == null) thanhToan.setMaGiaoDichVnpay(java.util.UUID.randomUUID().toString().replace("-", ""));
        String txnRef = thanhToan.getMaGiaoDichVnpay();
        thanhToan.setDatSan(datSan);
        thanhToan.setSoTien(soTien);
        thanhToan.setTongTien(soTien);
        thanhToan.setPhuongThuc(PhuongThucThanhToan.VNPAY);
        thanhToan.setTrangThai(TrangThaiThanhToan.CHO_THANH_TOAN);
        thanhToanRepository.save(thanhToan);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", vnPayConfig.getVersion());
        params.put("vnp_Command", vnPayConfig.getCommand());
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", soTien.multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
        params.put("vnp_CurrCode", vnPayConfig.getCurrencyCode());
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan dat san #" + datSanId);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime now = BookingGuard.now();
        params.put("vnp_CreateDate", now.format(formatter));
        params.put("vnp_ExpireDate", datSan.getThoiGianHetHanGiuCho().format(formatter));

        String query = VNPayUtil.buildQueryAndHash(params, vnPayConfig.getHashSecret());
        return vnPayConfig.getPayUrl() + "?" + query;
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean handleReturn(Map<String, String> params) {
        String code = processCallback(params);
        if (!"00".equals(code) && !"02".equals(code)) return false;
        ThanhToan payment = thanhToanRepository.findByMaGiaoDichVnpay(params.get("vnp_TxnRef")).orElse(null);
        return payment != null && payment.getTrangThai() == TrangThaiThanhToan.THANH_CONG
            && !payment.isCanXuLyHoanTien() && payment.getDatSan().getTrangThai() != TrangThaiDatSan.DA_HUY
            && "00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"));
    }

    public boolean validSignature(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null) return false;
        Map<String, String> fields = new HashMap<>(params);
        fields.remove("vnp_SecureHash"); fields.remove("vnp_SecureHashType");
        String query = VNPayUtil.buildQueryAndHash(fields, vnPayConfig.getHashSecret());
        String computedHash = query.substring(query.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
        if (!java.security.MessageDigest.isEqual(computedHash.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                receivedHash.toLowerCase(java.util.Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.US_ASCII))) return false;
        if (!vnPayConfig.getTmnCode().equals(params.get("vnp_TmnCode"))) return false;
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    public Map<String, String> handleIpn(Map<String, String> params) {
        String code = processCallback(params);
        return Map.of("RspCode", code, "Message", "00".equals(code) ? "Confirm Success" :
            "02".equals(code) ? "Order already confirmed" : "Callback rejected");
    }

    private String processCallback(Map<String, String> params) {
        if (!validSignature(params)) return "97";
        String txnRef = params.get("vnp_TxnRef");
        if (txnRef == null) return "01";
        ThanhToan payment = thanhToanRepository.findByMaGiaoDichVnpay(txnRef).orElse(null);
        if (payment == null || payment.getPhuongThuc() != PhuongThucThanhToan.VNPAY) return "01";
        DatSan booking = guard.lockBooking(payment.getDatSan().getId());
        entityManager.refresh(payment);
        try {
            BigDecimal amount = new BigDecimal(params.get("vnp_Amount"));
            if (payment.getSoTien() == null || payment.getTongTien() == null
                    || amount.compareTo(payment.getSoTien().multiply(BigDecimal.valueOf(100))) != 0
                    || payment.getSoTien().compareTo(booking.getGia()) != 0
                    || payment.getTongTien().compareTo(booking.getGia()) != 0) return "04";
        } catch (RuntimeException malformedAmount) { return "04"; }
        boolean success = "00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"))
                && params.get("vnp_TransactionNo") != null && params.get("vnp_TransactionNo").matches("[0-9]+");
        if (payment.getTrangThai() == TrangThaiThanhToan.THANH_CONG) return "02";
        // A verified late/cancelled settlement is recorded for manual refund review; never revives a booking.
        if (!success) {
            if (payment.getTrangThai() != TrangThaiThanhToan.CHO_THANH_TOAN) return "02";
            payment.setTrangThai(TrangThaiThanhToan.THAT_BAI);
            return "00";
        }
        if (payment.isCanXuLyHoanTien()) return "02";
        if (booking.getTrangThai() != TrangThaiDatSan.GIU_CHO_TAM || !BookingGuard.occupies(booking)) {
            payment.setCanXuLyHoanTien(true);
            payment.setThoiGianThanhToan(BookingGuard.now());
            return "00";
        }
        if (payment.getTrangThai() != TrangThaiThanhToan.CHO_THANH_TOAN) { payment.setCanXuLyHoanTien(true); return "00"; }
        payment.setTrangThai(TrangThaiThanhToan.THANH_CONG);
        payment.setThoiGianThanhToan(BookingGuard.now());
        booking.setTrangThai(TrangThaiDatSan.DA_XAC_NHAN);
        booking.setThoiGianHetHanGiuCho(null);
        events.publishEvent(new PaymentConfirmedEmail(booking.getId(), booking.getKhachHang().getEmail(),
            booking.getKhachHang().getHoTen(), booking.getSan().getTenSan(), booking.getNgayDat(),
            booking.getKhungGio().getGioBatDau(), booking.getKhungGio().getGioKetThuc(), payment.getSoTien()));
        return "00";
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> paymentStatus(String reference) {
        ThanhToan payment = thanhToanRepository.findByMaGiaoDichVnpay(reference)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        DatSan booking = payment.getDatSan();
        if (!booking.getKhachHang().getId().equals(BookingGuard.current().getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        String state = payment.isCanXuLyHoanTien() ? "REFUND_REVIEW" :
            booking.getTrangThai() == TrangThaiDatSan.DA_HUY ? "CANCELLED" :
            payment.getTrangThai() == TrangThaiThanhToan.THANH_CONG ? "SUCCESS" :
            payment.getTrangThai() == TrangThaiThanhToan.THAT_BAI ? "FAILED" : "VERIFYING";
        return Map.of("status", state, "bookingId", booking.getId(), "paymentStatus", payment.getTrangThai(),
            "bookingStatus", booking.getTrangThai(), "amount", payment.getSoTien());
    }
}
