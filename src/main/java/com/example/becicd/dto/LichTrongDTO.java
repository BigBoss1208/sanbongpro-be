package com.example.becicd.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public class LichTrongDTO {
    private Long khungGioId;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private BigDecimal gia;
    private String trangThai;

    public LichTrongDTO(Long khungGioId, LocalTime gioBatDau, LocalTime gioKetThuc, BigDecimal gia, String trangThai) {
        this.khungGioId = khungGioId;
        this.gioBatDau = gioBatDau;
        this.gioKetThuc = gioKetThuc;
        this.gia = gia;
        this.trangThai = trangThai;
    }

    public Long getKhungGioId() { return khungGioId; }
    public LocalTime getGioBatDau() { return gioBatDau; }
    public LocalTime getGioKetThuc() { return gioKetThuc; }
    public BigDecimal getGia() { return gia; }
    public String getTrangThai() { return trangThai; }
}