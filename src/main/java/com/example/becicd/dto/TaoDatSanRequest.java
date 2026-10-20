package com.example.becicd.dto;

import com.example.becicd.enums.PhuongThucThanhToan;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

public class TaoDatSanRequest {

    @NotNull(message = "Vui lòng chọn sân")
    private Long sanId;

    @NotNull(message = "Vui lòng chọn khung giờ")
    private Long khungGioId;

    @NotNull(message = "Vui lòng chọn ngày đặt")
    private LocalDate ngayDat;

    private PhuongThucThanhToan phuongThucThanhToan = PhuongThucThanhToan.VNPAY;

    public Long getSanId() { return sanId; }
    public void setSanId(Long sanId) { this.sanId = sanId; }
    public Long getKhungGioId() { return khungGioId; }
    public void setKhungGioId(Long khungGioId) { this.khungGioId = khungGioId; }
    public LocalDate getNgayDat() { return ngayDat; }
    public void setNgayDat(LocalDate ngayDat) { this.ngayDat = ngayDat; }
    public PhuongThucThanhToan getPhuongThucThanhToan() { return phuongThucThanhToan; }
    public void setPhuongThucThanhToan(PhuongThucThanhToan phuongThucThanhToan) { this.phuongThucThanhToan = phuongThucThanhToan; }
}