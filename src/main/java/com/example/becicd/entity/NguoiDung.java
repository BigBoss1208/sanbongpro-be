package com.example.becicd.entity;

import com.example.becicd.enums.VaiTro;
import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "nguoi_dung")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NguoiDung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false)
    private String matKhau;

    @Column(name = "ho_ten", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "sdt", length = 15)
    private String soDienThoai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cum_san_id")
    private CumSan cumSan;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private VaiTro vaiTro; // KHACH_HANG, NHAN_VIEN_PAGE, NHAN_VIEN_SAN, ADMIN

    @Convert(converter = ActiveStatusConverter.class)
    @Column(name = "khoa_tai_khoan", nullable = false)
    @Builder.Default
    private Boolean trangThai = true;

    // ALIAS METHODS (Giúp tương thích với Seeder / Controller / Security)
    public String getUsername() { return this.username != null ? this.username : this.email; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return this.matKhau; }
    public void setPassword(String password) { this.matKhau = password; }

    public String getSdt() { return this.soDienThoai; }
    public void setSdt(String sdt) { this.soDienThoai = sdt; }

    public VaiTro getRole() { return this.vaiTro; }
    public void setRole(VaiTro role) { this.vaiTro = role; }

    // trangThai: true = đang hoạt động, false = đã khóa
    public boolean isTrangThai() { return this.trangThai == null || this.trangThai; }
    public void setTrangThai(boolean trangThai) { this.trangThai = trangThai; }

    public Boolean getKhoaTaiKhoan() { return this.trangThai != null && !this.trangThai; }
    public void setKhoaTaiKhoan(boolean khoa) { this.trangThai = !khoa; }
}