package com.example.becicd.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "gia_san", uniqueConstraints = {
        // Thêm loai_ngay vào đây để cặp (san_id, khung_gio_id, loai_ngay) không bị trùng
        @UniqueConstraint(columnNames = {"san_id", "khung_gio_id", "loai_ngay"})
})
@Data
public class GiaSan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "san_id", nullable = false)
    private San san;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khung_gio_id", nullable = false)
    private KhungGio khungGio;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal gia;

    @Column(name = "loai_ngay", length = 20)
    private String loai_ngay = "TAT_CA";
}