// KhungGio.java
package com.example.becicd.entity;

import javax.persistence.*;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "khung_gio")
@Data
public class KhungGio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gio_bat_dau", nullable = false)
    private LocalTime gioBatDau;

    @Column(name = "gio_ket_thuc", nullable = false)
    private LocalTime gioKetThuc;

    @OneToMany(mappedBy = "khungGio")
    private List<GiaSan> danhSachGia;

    @OneToMany(mappedBy = "khungGio")
    private List<DatSan> danhSachDatSan;
}
