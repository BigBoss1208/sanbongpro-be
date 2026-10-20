// DatSan.java
package com.example.becicd.entity;

import com.example.becicd.enums.TrangThaiDatSan;
import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "dat_san")
@Data
public class DatSan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private NguoiDung khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_id", nullable = false)
    private San san;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khung_gio_id", nullable = false)
    private KhungGio khungGio;

    @Column(name = "ngay_dat", nullable = false)
    private LocalDate ngayDat;

    // Snapshot giá tại thời điểm đặt, để không bị ảnh hưởng nếu giá sân thay đổi sau này
    @Column(name = "gia", nullable = false, precision = 12, scale = 0)
    private BigDecimal gia;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiDatSan trangThai;

    @Column(name = "thoi_gian_het_han_giu_cho")
    private LocalDateTime thoiGianHetHanGiuCho;

    @Column(name = "ly_do_tu_choi", length = 500)
    private String lyDoTuChoi;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;

    @OneToMany(mappedBy = "datSan")
    private List<ThanhToan> danhSachThanhToan;

    @PrePersist
    public void truocKhiLuu() {
        this.ngayTao = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        if (this.trangThai == null) {
            this.trangThai = TrangThaiDatSan.GIU_CHO_TAM;
        }
    }
}
