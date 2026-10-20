package com.example.becicd.dto;

import com.example.becicd.entity.San;

import java.math.BigDecimal;
import java.util.Comparator;

public class SanDTO {

    private Long id;
    private String tenSan;
    private String loaiSan;
    private String hinhAnh;
    private String moTa;
    private String trangThai;
    private Long cumSanId;
    private String tenCumSan;
    // Giá thấp nhất trong các khung giờ đã cấu hình cho sân này (null nếu chưa có giá nào)
    private BigDecimal giaThapNhat;

    public static SanDTO tu(San s) {
        SanDTO dto = new SanDTO();
        dto.id = s.getId();
        dto.tenSan = s.getTenSan();
        dto.loaiSan = s.getLoaiSan();
        dto.hinhAnh = s.getHinhAnh();
        dto.moTa = s.getMoTa();
        dto.trangThai = s.getTrangThai();

        if (s.getCumSan() != null) {
            dto.cumSanId = s.getCumSan().getId();
            dto.tenCumSan = s.getCumSan().getTenCum();
        }

        if (s.getDanhSachGia() != null) {
            dto.giaThapNhat = s.getDanhSachGia().stream()
                    .map(gia -> gia.getGia())
                    .min(Comparator.naturalOrder())
                    .orElse(null);
        }

        return dto;
    }

    public Long getId() { return id; }
    public String getTenSan() { return tenSan; }
    public String getLoaiSan() { return loaiSan; }
    public String getHinhAnh() { return hinhAnh; }
    public String getMoTa() { return moTa; }
    public String getTrangThai() { return trangThai; }
    public Long getCumSanId() { return cumSanId; }
    public String getTenCumSan() { return tenCumSan; }
    public BigDecimal getGiaThapNhat() { return giaThapNhat; }
}