// San.java
package com.example.becicd.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "san")
@Data
public class San {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_san", nullable = false, length = 100)
    private String tenSan;

    @Column(name = "loai_san", length = 20)
    private String loaiSan;

    @Column(name = "hinh_anh")
    private String hinhAnh;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "trang_thai", length = 20)
    private String trangThai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cum_san_id")
    private CumSan cumSan;

    @OneToMany(mappedBy = "san")
    private List<GiaSan> danhSachGia;

    @OneToMany(mappedBy = "san")
    private List<DatSan> danhSachDatSan;
}
