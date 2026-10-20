package com.example.becicd.controller;

import com.example.becicd.dto.DoiMatKhauRequest;
import com.example.becicd.entity.NguoiDung;
import com.example.becicd.repository.NguoiDungRepository;
import com.example.becicd.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/nguoi-dung")
public class NguoiDungController {

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;

    public NguoiDungController(NguoiDungRepository nguoiDungRepository, PasswordEncoder passwordEncoder) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public ResponseEntity<?> thongTinCaNhan(@AuthenticationPrincipal CustomUserDetails currentUser) {
        NguoiDung nguoiDung = nguoiDungRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
        return ResponseEntity.ok(thongTinAnToan(nguoiDung));
    }

    @PutMapping("/me")
    public ResponseEntity<?> capNhatCaNhan(@AuthenticationPrincipal CustomUserDetails currentUser,
                                           @RequestBody Map<String, String> request) {
        NguoiDung nguoiDung = nguoiDungRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
        if (request.get("hoTen") != null) nguoiDung.setHoTen(com.example.becicd.dto.UserInput.name(request.get("hoTen")));
        if (request.get("soDienThoai") != null) nguoiDung.setSoDienThoai(com.example.becicd.dto.UserInput.phone(request.get("soDienThoai")));
        String email = request.containsKey("email") ? com.example.becicd.dto.UserInput.email(request.get("email")) : null;
        if (email != null && !email.equalsIgnoreCase(nguoiDung.getEmail())) {
            if (Boolean.TRUE.equals(nguoiDungRepository.existsByEmailIgnoreCase(email))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng");
            }
            nguoiDung.setEmail(email);
        }
        return ResponseEntity.ok(thongTinAnToan(nguoiDungRepository.save(nguoiDung)));
    }

    private Map<String, Object> thongTinAnToan(NguoiDung nguoiDung) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", nguoiDung.getId());
        result.put("username", nguoiDung.getUsername());
        result.put("hoTen", nguoiDung.getHoTen());
        result.put("email", nguoiDung.getEmail());
        result.put("soDienThoai", nguoiDung.getSoDienThoai());
        result.put("role", nguoiDung.getRole() != null ? nguoiDung.getRole().name() : "KHACH_HANG");
        return result;
    }

    @PutMapping("/doi-mat-khau")
    public ResponseEntity<String> doiMatKhau(@AuthenticationPrincipal CustomUserDetails currentUser,
                                             @RequestBody DoiMatKhauRequest request) {
        NguoiDung nguoiDung = nguoiDungRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));

        if (request.getMatKhauCu() == null || request.getMatKhauCu().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhập mật khẩu cũ");
        com.example.becicd.dto.UserInput.password(request.getMatKhauMoi());
        if (!passwordEncoder.matches(request.getMatKhauCu(), nguoiDung.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu cũ không đúng");
        }
        if (request.getMatKhauMoi() == null || request.getMatKhauMoi().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải từ 8 ký tự trở lên");
        }

        nguoiDung.setPassword(passwordEncoder.encode(request.getMatKhauMoi()));
        nguoiDungRepository.save(nguoiDung);
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }
}
