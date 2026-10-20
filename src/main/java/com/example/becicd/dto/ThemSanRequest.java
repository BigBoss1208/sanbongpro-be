package com.example.becicd.dto;

public class ThemSanRequest {
    private String tenSan;
    private String loaiSan;
    private String hinhAnh;
    private String moTa;
    private Long cumSanId;

    public String getTenSan() { return tenSan; }
    public void setTenSan(String tenSan) { this.tenSan = tenSan; }
    public String getLoaiSan() { return loaiSan; }
    public void setLoaiSan(String loaiSan) { this.loaiSan = loaiSan; }
    public String getHinhAnh() { return hinhAnh; }
    public void setHinhAnh(String hinhAnh) { this.hinhAnh = hinhAnh; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public Long getCumSanId() { return cumSanId; }
    public void setCumSanId(Long cumSanId) { this.cumSanId = cumSanId; }
}