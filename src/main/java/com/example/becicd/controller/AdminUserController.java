package com.example.becicd.controller;

import com.example.becicd.entity.NguoiDung;
import com.example.becicd.enums.VaiTro;
import com.example.becicd.repository.NguoiDungRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")

public class AdminUserController {

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(NguoiDungRepository nguoiDungRepository, PasswordEncoder passwordEncoder) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. Lấy danh sách Khách hàng
    @GetMapping("/khach-hang")
    public ResponseEntity<List<Map<String, Object>>> getKhachHang() {
        return ResponseEntity.ok(nguoiDungRepository.findByVaiTro(VaiTro.KHACH_HANG)
                .stream().map(this::thongTinAnToan).collect(java.util.stream.Collectors.toList()));
    }

    // 2. Lấy danh sách Nhân viên
    @GetMapping("/nhan-vien")
    public ResponseEntity<List<Map<String, Object>>> getNhanVien() {
        return ResponseEntity.ok(nguoiDungRepository.findByVaiTroIn(List.of(VaiTro.NHAN_VIEN_PAGE, VaiTro.NHAN_VIEN_SAN, VaiTro.ADMIN))
                .stream().map(this::thongTinAnToan).collect(java.util.stream.Collectors.toList()));
    }

    @PostMapping("/khach-hang")
    public ResponseEntity<Map<String, Object>> taoKhachHang(@RequestBody Map<String, String> request) {
        return taoNguoiDung(request, VaiTro.KHACH_HANG);
    }

    @PostMapping("/nhan-vien")
    public ResponseEntity<Map<String, Object>> taoNhanVien(@RequestBody Map<String, String> request) {
        String rawRole = request.get("vaiTro");
        VaiTro role;
        try {
            role = rawRole == null ? VaiTro.NHAN_VIEN_PAGE : VaiTro.valueOf(rawRole);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò nhân viên không hợp lệ");
        }
        if (role != VaiTro.NHAN_VIEN_PAGE && role != VaiTro.NHAN_VIEN_SAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được tạo tài khoản nhân viên");
        }
        return taoNguoiDung(request, role);
    }

    private ResponseEntity<Map<String, Object>> taoNguoiDung(Map<String, String> request, VaiTro role) {
        String email = request.get("email");
        String hoTen = request.get("hoTen");
        String password = request.get("password");
        email = com.example.becicd.dto.UserInput.email(email);
        com.example.becicd.dto.UserInput.username(email);
        hoTen = com.example.becicd.dto.UserInput.name(hoTen);
        password = com.example.becicd.dto.UserInput.password(password);
        String phone = com.example.becicd.dto.UserInput.phone(request.get("sdt"));
        if (email == null || email.isBlank() || hoTen == null || hoTen.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email và họ tên là bắt buộc");
        }
        if (password == null || password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải có ít nhất 8 ký tự");
        }
        if (Boolean.TRUE.equals(nguoiDungRepository.existsByEmailIgnoreCase(email))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }
        if (Boolean.TRUE.equals(nguoiDungRepository.existsByUsername(email))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên đăng nhập đã được sử dụng");
        }

        NguoiDung user = NguoiDung.builder()
                .username(email)
                .email(email)
                .matKhau(passwordEncoder.encode(password))
                .hoTen(hoTen.trim())
                .soDienThoai(phone)
                .vaiTro(role)
                .trangThai(true)
                .build();
        NguoiDung saved = nguoiDungRepository.save(user);
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("id", saved.getId());
        response.put("username", saved.getUsername());
        response.put("email", saved.getEmail());
        response.put("hoTen", saved.getHoTen());
        response.put("sdt", saved.getSdt());
        response.put("vaiTro", saved.getRole().name());
        response.put("trangThai", saved.isTrangThai());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 3. Khóa / Mở tài khoản người dùng
    @PutMapping("/nguoi-dung/{id}/khoa")
    public ResponseEntity<?> toggleKhoaAccount(@PathVariable Long id) {
        return nguoiDungRepository.findById(id).map(user -> {
            user.setTrangThai(!user.isTrangThai());
            nguoiDungRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái thành công!", "trangThai", user.isTrangThai()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping({"/khach-hang/{id}", "/nhan-vien/{id}"})
    public ResponseEntity<?> capNhatNguoiDung(@PathVariable Long id, @RequestBody Map<String, String> request) {
        return nguoiDungRepository.findById(id).map(user -> {
            if (request.get("hoTen") != null) user.setHoTen(com.example.becicd.dto.UserInput.name(request.get("hoTen")));
            if (request.containsKey("email")) {
                String email = com.example.becicd.dto.UserInput.email(request.get("email"));
                if (!email.equalsIgnoreCase(user.getEmail()) && Boolean.TRUE.equals(nguoiDungRepository.existsByEmailIgnoreCase(email))) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng");
                user.setEmail(email);
            }
            if (request.get("sdt") != null) user.setSdt(com.example.becicd.dto.UserInput.phone(request.get("sdt")));
            if (request.get("vaiTro") != null && !request.get("vaiTro").equals(user.getVaiTro().name())) {
                VaiTro role;
                try { role = VaiTro.valueOf(request.get("vaiTro")); } catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò không hợp lệ"); }
                if ((user.getVaiTro() != VaiTro.NHAN_VIEN_PAGE && user.getVaiTro() != VaiTro.NHAN_VIEN_SAN) || (role != VaiTro.NHAN_VIEN_PAGE && role != VaiTro.NHAN_VIEN_SAN)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được đổi giữa vai trò nhân viên");
                user.setRole(role);
            }
            return ResponseEntity.ok(thongTinAnToan(nguoiDungRepository.save(user)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping({"/khach-hang/{id}", "/nhan-vien/{id}"})
    public ResponseEntity<?> xoaNguoiDung(@PathVariable Long id) {
        if (!nguoiDungRepository.existsById(id)) return ResponseEntity.notFound().build();
        nguoiDungRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> thongTinAnToan(NguoiDung user) {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("hoTen", user.getHoTen());
        response.put("sdt", user.getSdt());
        response.put("vaiTro", user.getRole() == null ? null : user.getRole().name());
        response.put("trangThai", user.isTrangThai());
        response.put("cumSanId", user.getCumSan() == null ? null : user.getCumSan().getId());
        return response;
    }
}
