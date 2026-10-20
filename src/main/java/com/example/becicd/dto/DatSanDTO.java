package com.example.becicd.dto;

import com.example.becicd.entity.DatSan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DatSanDTO {

    private Long id;
    private Long sanId;
    private String tenSan;
    private Long khungGioId;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private LocalDate ngayDat;
    private BigDecimal gia;
    private String trangThai;
    private String trangThaiThanhToan;
    private boolean canXuLyHoanTien;
    private String tenKhachHang;
    private String soDienThoai;
    private String lyDoTuChoi;
    private LocalDateTime thoiGianHetHanGiuCho;
    private LocalDateTime ngayTao;

    public static DatSanDTO tu(DatSan d) {
        DatSanDTO dto = new DatSanDTO();
        dto.id = d.getId();
        dto.sanId = d.getSan().getId();
        dto.tenSan = d.getSan().getTenSan();
        dto.khungGioId = d.getKhungGio().getId();
        dto.gioBatDau = d.getKhungGio().getGioBatDau();
        dto.gioKetThuc = d.getKhungGio().getGioKetThuc();
        dto.ngayDat = d.getNgayDat();
        dto.gia = d.getGia();
        dto.trangThai = d.getTrangThai().name();
        dto.tenKhachHang = d.getKhachHang() != null ? d.getKhachHang().getHoTen() : "Khách vãng lai";
        dto.soDienThoai = d.getKhachHang() != null ? d.getKhachHang().getSoDienThoai() : null;
        dto.lyDoTuChoi = d.getLyDoTuChoi();
        dto.thoiGianHetHanGiuCho = d.getThoiGianHetHanGiuCho();
        dto.ngayTao = d.getNgayTao();
        return dto;
    }

    public void setPayment(com.example.becicd.entity.ThanhToan payment) {
        if (payment != null) {
            trangThaiThanhToan = payment.getTrangThai().name();
            canXuLyHoanTien = payment.isCanXuLyHoanTien();
        }
    }
    public String getTrangThaiThanhToan() { return trangThaiThanhToan; }
    public boolean isCanXuLyHoanTien() { return canXuLyHoanTien; }
    public Long getId() { return id; }
    public Long getSanId() { return sanId; }
    public String getTenSan() { return tenSan; }
    public Long getKhungGioId() { return khungGioId; }
    public LocalTime getGioBatDau() { return gioBatDau; }
    public LocalTime getGioKetThuc() { return gioKetThuc; }
    public LocalDate getNgayDat() { return ngayDat; }
    public BigDecimal getGia() { return gia; }
    public String getTrangThai() { return trangThai; }
    public String getTenKhachHang() { return tenKhachHang; }
    public String getSoDienThoai() { return soDienThoai; }
    public String getLyDoTuChoi() { return lyDoTuChoi; }
    public LocalDateTime getThoiGianHetHanGiuCho() { return thoiGianHetHanGiuCho; }
    public LocalDateTime getNgayTao() { return ngayTao; }
}