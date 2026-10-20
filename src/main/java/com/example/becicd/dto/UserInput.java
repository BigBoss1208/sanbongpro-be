package com.example.becicd.dto;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Shared validation for map-based legacy user contracts. */
public final class UserInput {
    private UserInput() { }
    public static String name(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 100) bad("Họ tên bắt buộc, tối đa 100 ký tự");
        return value.trim();
    }
    public static String email(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 100 || !value.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) bad("Email không hợp lệ");
        return value.trim();
    }
    public static String username(String value) {
        if (value == null || value.isBlank() || value.length() > 50 || value.chars().anyMatch(Character::isWhitespace)) bad("Tên đăng nhập bắt buộc, không có khoảng trắng, tối đa 50 ký tự");
        return value;
    }
    public static String phone(String value) {
        if (value == null || value.isBlank()) return null;
        String phone = value.trim();
        if (!phone.matches("\\+?[0-9]{9,14}")) bad("Số điện thoại không hợp lệ");
        return phone;
    }
    public static String password(String value) {
        if (value == null || value.isBlank() || value.length() < 8 || value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) bad("Mật khẩu từ 8 ký tự và tối đa 72 byte");
        return value;
    }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
