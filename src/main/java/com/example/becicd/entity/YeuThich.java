package com.example.becicd.entity;

import javax.persistence.*;

@Entity
@Table(name = "san_yeu_thich", uniqueConstraints = @UniqueConstraint(columnNames = {"nguoi_dung_id", "san_id"}))
public class YeuThich {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "san_id", nullable = false)
    private San san;

    public Long getId() { return id; }
    public NguoiDung getNguoiDung() { return nguoiDung; }
    public void setNguoiDung(NguoiDung nguoiDung) { this.nguoiDung = nguoiDung; }
    public San getSan() { return san; }
    public void setSan(San san) { this.san = san; }
}