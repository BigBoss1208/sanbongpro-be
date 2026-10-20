package com.example.becicd.dto;

import com.example.becicd.enums.PhuongThucThanhToan;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

public class TaoWalkinDatSanRequest {

    @NotNull(message = "Vui lòng chọn sân")
    private Long sanId;

    @NotNull(message = "Vui lòng chọn khung giờ")
    private Long khungGioId;

    @NotNull(message = "Vui lòng chọn ngày đặt")
    private LocalDate ngayDat;

    @NotBlank(message = "Vui lòng nhập tên khách hàng")
    @Size(max = 100, message = "Tên khách hàng tối đa 100 ký tự")
    private String tenKhachHang;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Size(max = 15, message = "Số điện thoại tối đa 15 ký tự")
    private String soDienThoai;

    private PhuongThucThanhToan phuongThucThanhToan = PhuongThucThanhToan.TIEN_MAT;

    public Long getSanId() { return sanId; }
    public void setSanId(Long sanId) { this.sanId = sanId; }
    public Long getKhungGioId() { return khungGioId; }
    public void setKhungGioId(Long khungGioId) { this.khungGioId = khungGioId; }
    public LocalDate getNgayDat() { return ngayDat; }
    public void setNgayDat(LocalDate ngayDat) { this.ngayDat = ngayDat; }
    public String getTenKhachHang() { return tenKhachHang; }
    public void setTenKhachHang(String tenKhachHang) { this.tenKhachHang = tenKhachHang; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public PhuongThucThanhToan getPhuongThucThanhToan() { return phuongThucThanhToan; }
    public void setPhuongThucThanhToan(PhuongThucThanhToan phuongThucThanhToan) { this.phuongThucThanhToan = phuongThucThanhToan; }
}
