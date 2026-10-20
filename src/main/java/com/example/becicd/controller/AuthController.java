package com.example.becicd.controller;

import com.example.becicd.entity.NguoiDung;
import com.example.becicd.enums.VaiTro;
import com.example.becicd.repository.NguoiDungRepository;
import com.example.becicd.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")

public class AuthController {

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthController(NguoiDungRepository nguoiDungRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/dang-nhap")
    public ResponseEntity<?> dangNhap(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
        }

        // 1. Tìm người dùng theo Username, Email hoặc SĐT
        NguoiDung nguoiDung = nguoiDungRepository.findAll().stream()
                .filter(u -> username.equals(u.getUsername()) || username.equals(u.getEmail()) || username.equals(u.getSdt()) || username.equals(u.getSoDienThoai()))
                .findFirst()
                .orElse(null);

        if (nguoiDung == null || !passwordEncoder.matches(password, nguoiDung.getPassword())) {
            return ResponseEntity.status(401).body("Sai tài khoản hoặc mật khẩu");
        }

        if (Boolean.TRUE.equals(nguoiDung.getKhoaTaiKhoan())) {
            return ResponseEntity.status(403).body("Tài khoản đã bị khóa!");
        }

        // 2. Xác định vai trò chuẩn
        VaiTro vt = nguoiDung.getRole();
        String roleStr = (vt != null) ? vt.name() : "KHACH_HANG";

        // 3. Tạo Token JWT
        String token = tokenProvider.generateToken(nguoiDung.getUsername(), roleStr);

        // 4. Đóng gói Response đa dạng chuẩn dữ liệu cho mọi component React
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("accessToken", token);
        response.put("username", nguoiDung.getUsername());
        response.put("hoTen", nguoiDung.getHoTen());
        response.put("email", nguoiDung.getEmail());

        // Trả về Role phẳng
        response.put("role", roleStr);
        response.put("vaiTro", roleStr);
        response.put("roles", List.of(roleStr, "ROLE_" + roleStr));

        // Bổ sung Object 'user' lồng nhau dành cho Redux / AuthContext phía FE
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", nguoiDung.getId());
        userInfo.put("username", nguoiDung.getUsername());
        userInfo.put("hoTen", nguoiDung.getHoTen());
        userInfo.put("email", nguoiDung.getEmail());
        userInfo.put("role", roleStr);
        userInfo.put("vaiTro", roleStr);
        userInfo.put("roles", List.of(roleStr, "ROLE_" + roleStr));

        response.put("user", userInfo);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/dang-ky")
    public ResponseEntity<?> dangKy(@RequestBody Map<String, String> req) {
        String email = req.get("email") != null ? req.get("email") : req.get("username");
        String password = req.get("password") != null ? req.get("password") : req.get("matKhau");
        String hoTen = req.get("hoTen");
        String sdt = req.get("sdt") != null ? req.get("sdt") : req.get("soDienThoai");

        email = com.example.becicd.dto.UserInput.email(email);
        com.example.becicd.dto.UserInput.username(email);
        password = com.example.becicd.dto.UserInput.password(password);
        hoTen = com.example.becicd.dto.UserInput.name(hoTen);
        sdt = com.example.becicd.dto.UserInput.phone(sdt);
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("Vui lòng nhập đầy đủ email và mật khẩu!");
        }
        if (password.length() < 8) {
            return ResponseEntity.badRequest().body("Mật khẩu phải từ 8 ký tự trở lên");
        }
        if (Boolean.TRUE.equals(nguoiDungRepository.existsByEmailIgnoreCase(email))) {
            return ResponseEntity.status(409).body("Email đã được sử dụng");
        }
        if (Boolean.TRUE.equals(nguoiDungRepository.existsByUsername(email))) {
            return ResponseEntity.status(409).body("Tên đăng nhập đã được sử dụng");
        }

        NguoiDung nguoiDung = NguoiDung.builder()
            .username(email)
                .email(email)
                .matKhau(passwordEncoder.encode(password))
                .hoTen(hoTen)
                .soDienThoai(sdt)
                .vaiTro(VaiTro.KHACH_HANG)
                .trangThai(true)
                .build();
        nguoiDungRepository.save(nguoiDung);

        return ResponseEntity.status(201).body(Map.of("message", "Đăng ký thành công"));
    }
}
