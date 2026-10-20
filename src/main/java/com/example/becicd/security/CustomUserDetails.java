package com.example.becicd.security;

import com.example.becicd.entity.NguoiDung;
import com.example.becicd.enums.VaiTro;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final NguoiDung nguoiDung;

    public CustomUserDetails(NguoiDung nguoiDung) {
        this.nguoiDung = nguoiDung;
    }

    public NguoiDung getNguoiDung() {
        return nguoiDung;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        VaiTro role = nguoiDung.getRole() != null ? nguoiDung.getRole() : VaiTro.KHACH_HANG;
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return nguoiDung.getPassword(); }

    @Override
    public String getUsername() { return nguoiDung.getUsername(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return !Boolean.TRUE.equals(nguoiDung.getKhoaTaiKhoan()); }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}