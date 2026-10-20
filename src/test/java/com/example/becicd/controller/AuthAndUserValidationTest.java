package com.example.becicd.controller;

import com.example.becicd.repository.NguoiDungRepository;
import com.example.becicd.security.JwtTokenProvider;
import com.example.becicd.entity.NguoiDung;
import com.example.becicd.enums.VaiTro;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AuthAndUserValidationTest {
    private Map<String,String> registration() { return new HashMap<>(Map.of("email", "customer@example.test", "password", "valid-password", "hoTen", "Customer", "sdt", "0901234567")); }
    @Test void registrationAlwaysCreatesCustomerWithEncodedPassword() {
        NguoiDungRepository repository = mock(NguoiDungRepository.class);
        AuthController controller = new AuthController(repository, new BCryptPasswordEncoder(), mock(JwtTokenProvider.class));
        Map<String,String> input = registration(); input.put("vaiTro", "ADMIN");
        assertEquals(201, controller.dangKy(input).getStatusCodeValue());
        var saved = org.mockito.ArgumentCaptor.forClass(NguoiDung.class); verify(repository).save(saved.capture());
        assertEquals(VaiTro.KHACH_HANG, saved.getValue().getVaiTro());
        assertTrue(new BCryptPasswordEncoder().matches("valid-password", saved.getValue().getMatKhau()));
    }
    @Test void duplicateAndMalformedRegistrationsDoNotWrite() {
        NguoiDungRepository repository = mock(NguoiDungRepository.class); AuthController controller = new AuthController(repository, new BCryptPasswordEncoder(), mock(JwtTokenProvider.class));
        when(repository.existsByEmailIgnoreCase("customer@example.test")).thenReturn(true);
        assertEquals(409, controller.dangKy(registration()).getStatusCodeValue());
        var bad = registration(); bad.put("hoTen", " "); assertThrows(ResponseStatusException.class, () -> controller.dangKy(bad));
        verify(repository, never()).save(any());
    }
    @Test void lockedAndIncorrectLoginAreRejected() {
        NguoiDungRepository repository = mock(NguoiDungRepository.class); BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        NguoiDung user = NguoiDung.builder().username("customer").matKhau(encoder.encode("valid-password")).vaiTro(VaiTro.KHACH_HANG).trangThai(false).build();
        when(repository.findAll()).thenReturn(List.of(user)); JwtTokenProvider provider = mock(JwtTokenProvider.class);
        AuthController controller = new AuthController(repository, encoder, provider);
        assertEquals(403, controller.dangNhap(Map.of("username", "customer", "password", "valid-password")).getStatusCodeValue());
        assertEquals(401, controller.dangNhap(Map.of("username", "customer", "password", "wrong")).getStatusCodeValue());
        verifyNoInteractions(provider);
    }
    @Test void adminCannotPromoteCustomerToAdmin() {
        NguoiDungRepository repository = mock(NguoiDungRepository.class); NguoiDung user = NguoiDung.builder().vaiTro(VaiTro.KHACH_HANG).build(); when(repository.findById(1L)).thenReturn(Optional.of(user));
        AdminUserController controller = new AdminUserController(repository, new BCryptPasswordEncoder());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> controller.capNhatNguoiDung(1L, Map.of("vaiTro", "ADMIN"))).getStatus().value()); verify(repository, never()).save(any());
    }
}
