package com.example.becicd.entity;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "cum_san")
public class CumSan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_cum", nullable = false, length = 100)
    private String tenCum;

    @Column(name = "dia_chi", length = 255)
    private String diaChi;

    @Column(columnDefinition = "TEXT")
    private String moTa;

    @OneToMany(mappedBy = "cumSan")
    private List<San> danhSachSan;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenCum() { return tenCum; }
    public void setTenCum(String tenCum) { this.tenCum = tenCum; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public List<San> getDanhSachSan() { return danhSachSan; }
    public void setDanhSachSan(List<San> danhSachSan) { this.danhSachSan = danhSachSan; }
}
