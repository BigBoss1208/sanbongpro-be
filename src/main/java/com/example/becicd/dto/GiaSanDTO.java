package com.example.becicd.dto;

import com.example.becicd.entity.GiaSan;

import java.math.BigDecimal;
import java.time.LocalTime;

public class GiaSanDTO {

    private Long id;
    private Long sanId;
    private String tenSan;
    private Long khungGioId;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private BigDecimal gia;
    private String loaiNgay;

    public static GiaSanDTO tu(GiaSan g) {
        GiaSanDTO dto = new GiaSanDTO();
        dto.id = g.getId();
        dto.sanId = g.getSan().getId();
        dto.tenSan = g.getSan().getTenSan();
        dto.khungGioId = g.getKhungGio().getId();
        dto.gioBatDau = g.getKhungGio().getGioBatDau();
        dto.gioKetThuc = g.getKhungGio().getGioKetThuc();
        dto.gia = g.getGia();
        dto.loaiNgay = g.getLoai_ngay();
        return dto;
    }

    public Long getId() { return id; }
    public Long getSanId() { return sanId; }
    public String getTenSan() { return tenSan; }
    public Long getKhungGioId() { return khungGioId; }
    public LocalTime getGioBatDau() { return gioBatDau; }
    public LocalTime getGioKetThuc() { return gioKetThuc; }
    public BigDecimal getGia() { return gia; }
    public String getLoaiNgay() { return loaiNgay; }
}