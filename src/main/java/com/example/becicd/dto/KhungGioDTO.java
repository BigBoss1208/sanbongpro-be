package com.example.becicd.dto;

import com.example.becicd.entity.KhungGio;

import java.time.LocalTime;

public class KhungGioDTO {

    private Long id;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;

    public static KhungGioDTO tu(KhungGio k) {
        KhungGioDTO dto = new KhungGioDTO();
        dto.id = k.getId();
        dto.gioBatDau = k.getGioBatDau();
        dto.gioKetThuc = k.getGioKetThuc();
        return dto;
    }

    public Long getId() { return id; }
    public LocalTime getGioBatDau() { return gioBatDau; }
    public LocalTime getGioKetThuc() { return gioKetThuc; }
}