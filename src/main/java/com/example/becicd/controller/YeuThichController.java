package com.example.becicd.controller;

import com.example.becicd.dto.SanDTO;
import com.example.becicd.entity.San;
import com.example.becicd.entity.YeuThich;
import com.example.becicd.repository.SanRepository;
import com.example.becicd.repository.YeuThichRepository;
import com.example.becicd.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/yeu-thich")
@PreAuthorize("hasRole('KHACH_HANG')")
public class YeuThichController {

    private final YeuThichRepository yeuThichRepository;
    private final SanRepository sanRepository;

    public YeuThichController(YeuThichRepository yeuThichRepository, SanRepository sanRepository) {
        this.yeuThichRepository = yeuThichRepository;
        this.sanRepository = sanRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<SanDTO> danhSach(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return yeuThichRepository.findByNguoiDungIdOrderByIdDesc(currentUser.getNguoiDung().getId())
                .stream().map(YeuThich::getSan).map(SanDTO::tu).collect(Collectors.toList());
    }

    @PostMapping("/{sanId}")
    @Transactional
    public ResponseEntity<SanDTO> them(@PathVariable Long sanId,
                                       @AuthenticationPrincipal CustomUserDetails currentUser) {
        Long userId = currentUser.getNguoiDung().getId();
        if (!yeuThichRepository.findByNguoiDungIdAndSanId(userId, sanId).isPresent()) {
            San san = sanRepository.findById(sanId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân"));
            YeuThich favorite = new YeuThich();
            favorite.setNguoiDung(currentUser.getNguoiDung());
            favorite.setSan(san);
            yeuThichRepository.save(favorite);
        }
        San san = sanRepository.findById(sanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân"));
        return ResponseEntity.ok(SanDTO.tu(san));
    }

    @DeleteMapping("/{sanId}")
    @Transactional
    public ResponseEntity<Void> xoa(@PathVariable Long sanId,
                                    @AuthenticationPrincipal CustomUserDetails currentUser) {
        yeuThichRepository.findByNguoiDungIdAndSanId(currentUser.getNguoiDung().getId(), sanId)
                .ifPresent(yeuThichRepository::delete);
        return ResponseEntity.noContent().build();
    }
}