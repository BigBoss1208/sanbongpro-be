package com.example.becicd.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatrixLichSanDTO {
    private Long sanId;
    private String tenSan;
    private String loaiSan;
    private List<SlotItemDTO> slots;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SlotItemDTO {
        private Long khungGioId;
        private String gioBatDau;
        private String gioKetThuc;
        private BigDecimal gia;
        private String trangThai; // "TRONG", "DA_DAT", "CHO_THANH_TOAN"
        private String tenKhachHang; // Nếu đã đặt thì hiện tên khách (cho Staff)
        private String soDienThoai;
        private Long datSanId;
    }
}