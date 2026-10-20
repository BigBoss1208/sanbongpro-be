package com.example.becicd.entity;

import com.example.becicd.enums.PhuongThucThanhToan;
import com.example.becicd.enums.TrangThaiThanhToan;
import javax.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "thanh_toan")
@Data
public class ThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dat_san_id", nullable = false)
    private DatSan datSan;

    @Column(name = "so_tien", nullable = false, precision = 12, scale = 0)
    private BigDecimal soTien;

    @Column(name = "tong_tien", nullable = false, precision = 12, scale = 0)
    private BigDecimal tongTien;

    @Enumerated(EnumType.STRING)
    @Column(name = "phuong_thuc", nullable = false, length = 20)
    private PhuongThucThanhToan phuongThuc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiThanhToan trangThai;

    @Column(name = "ma_giao_dich_vnpay", length = 100, unique = true)
    private String maGiaoDichVnpay;

    @Column(name = "can_xu_ly_hoan_tien")
    private boolean canXuLyHoanTien;

    @Column(name = "thoi_gian_thanh_toan")
    private LocalDateTime thoiGianThanhToan;
}
