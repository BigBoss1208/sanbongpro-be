package com.example.becicd.controller;

import com.example.becicd.dto.SanDTO;
import com.example.becicd.repository.SanRepository;
import com.example.becicd.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/nhan-vien/san")
public class SanController {

    private final SanRepository sanRepository;

    public SanController(SanRepository sanRepository) {
        this.sanRepository = sanRepository;
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'NHAN_VIEN_SAN', 'NHAN_VIEN_PAGE')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<List<SanDTO>> xemDanhSachSan(@AuthenticationPrincipal CustomUserDetails user) {
        // Nhân viên chỉ xem cụm được phân công; admin có quyền toàn hệ thống.
        List<com.example.becicd.entity.San> danhSach = sanRepository.findAll().stream().filter(com.example.becicd.service.BookingGuard::mayAccess).collect(Collectors.toList());
        return ResponseEntity.ok(danhSach.stream().map(SanDTO::tu).collect(Collectors.toList()));
    }
}
