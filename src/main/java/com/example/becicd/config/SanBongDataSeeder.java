package com.example.becicd.config;

import com.example.becicd.entity.CumSan;
import com.example.becicd.entity.GiaSan;
import com.example.becicd.entity.KhungGio;
import com.example.becicd.entity.NguoiDung;
import com.example.becicd.entity.San;
import com.example.becicd.enums.VaiTro;
import com.example.becicd.repository.CumSanRepository;
import com.example.becicd.repository.GiaSanRepository;
import com.example.becicd.repository.KhungGioRepository;
import com.example.becicd.repository.NguoiDungRepository;
import com.example.becicd.repository.SanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * Seed dữ liệu mẫu khi ứng dụng khởi động lần đầu (DB rỗng).
 * Tạo: 1 cụm sân, vài sân, khung giờ, bảng giá và 4 tài khoản demo.
 */
@Component
@org.springframework.core.annotation.Order(1)
@org.springframework.context.annotation.Profile("dev")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "demo.seed.enabled", havingValue = "true")
public class SanBongDataSeeder implements CommandLineRunner {

    private final CumSanRepository cumSanRepository;
    private final SanRepository sanRepository;
    private final KhungGioRepository khungGioRepository;
    private final GiaSanRepository giaSanRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;

    public SanBongDataSeeder(CumSanRepository cumSanRepository,
                              SanRepository sanRepository,
                              KhungGioRepository khungGioRepository,
                              GiaSanRepository giaSanRepository,
                              NguoiDungRepository nguoiDungRepository,
                              PasswordEncoder passwordEncoder) {
        this.cumSanRepository = cumSanRepository;
        this.sanRepository = sanRepository;
        this.khungGioRepository = khungGioRepository;
        this.giaSanRepository = giaSanRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedTaiKhoanDemo();
        seedSanVaGia();
    }

    @org.springframework.beans.factory.annotation.Value("${DEMO_PASSWORD:}")
    private String demoPassword;

    private void seedTaiKhoanDemo() {
        if (nguoiDungRepository.count() > 0) {
            return;
        }

        if (demoPassword == null || demoPassword.length() < 12) {
            throw new IllegalStateException("DEMO_PASSWORD must contain at least 12 characters when demo seeding is enabled");
        }

        taoTaiKhoan("admin@sanbong.vn", demoPassword, "Quản trị viên", "0900000001", VaiTro.ADMIN);
        taoTaiKhoan("nhanvienpage@sanbong.vn", demoPassword, "Nhân viên Page", "0900000002", VaiTro.NHAN_VIEN_PAGE);
        taoTaiKhoan("nhanviensan@sanbong.vn", demoPassword, "Nhân viên Sân", "0900000003", VaiTro.NHAN_VIEN_SAN);
        taoTaiKhoan("khachhang@sanbong.vn", demoPassword, "Khách Hàng Demo", "0900000004", VaiTro.KHACH_HANG);
    }

    private void taoTaiKhoan(String email, String matKhauGoc, String hoTen, String sdt, VaiTro vaiTro) {
        NguoiDung nguoiDung = NguoiDung.builder()
            .username(email)
                .email(email)
                .matKhau(passwordEncoder.encode(matKhauGoc))
                .hoTen(hoTen)
                .soDienThoai(sdt)
                .vaiTro(vaiTro)
                .trangThai(true)
                .build();
        nguoiDungRepository.save(nguoiDung);
    }

    private void seedSanVaGia() {
        if (sanRepository.count() > 0) {
            return;
        }

        CumSan cumSan = new CumSan();
        cumSan.setTenCum("Cụm sân bóng Trung Tâm");
        cumSan.setDiaChi("123 Đường Thể Thao, Thái Nguyên");
        cumSan.setMoTa("Cụm sân bóng mini cỏ nhân tạo tiêu chuẩn");
        cumSan = cumSanRepository.save(cumSan);

        San sanA = new San();
        sanA.setTenSan("Sân A - 5 người");
        sanA.setLoaiSan("Sân 5");
        sanA.setMoTa("Sân cỏ nhân tạo có mái che");
        sanA.setTrangThai("HOAT_DONG");
        sanA.setCumSan(cumSan);
        sanA = sanRepository.save(sanA);

        San sanB = new San();
        sanB.setTenSan("Sân B - 7 người");
        sanB.setLoaiSan("Sân 7");
        sanB.setMoTa("Sân cỏ nhân tạo ngoài trời, hệ thống đèn chiếu sáng");
        sanB.setTrangThai("HOAT_DONG");
        sanB.setCumSan(cumSan);
        sanB = sanRepository.save(sanB);

        List<KhungGio> khungGioList = List.of(
                taoKhungGio(LocalTime.of(6, 0), LocalTime.of(7, 30)),
                taoKhungGio(LocalTime.of(7, 30), LocalTime.of(9, 0)),
                taoKhungGio(LocalTime.of(17, 0), LocalTime.of(18, 30)),
                taoKhungGio(LocalTime.of(18, 30), LocalTime.of(20, 0)),
                taoKhungGio(LocalTime.of(20, 0), LocalTime.of(21, 30))
        );

        for (San san : List.of(sanA, sanB)) {
            BigDecimal giaThuong = "Sân 5".equals(san.getLoaiSan()) ? new BigDecimal("300000") : new BigDecimal("500000");
            BigDecimal giaCuoiTuan = "Sân 5".equals(san.getLoaiSan()) ? new BigDecimal("400000") : new BigDecimal("650000");

            for (KhungGio khungGio : khungGioList) {
                boolean laKhungGioCao = khungGio.getGioBatDau().isAfter(LocalTime.of(16, 0));

                GiaSan giaNgayThuong = new GiaSan();
                giaNgayThuong.setSan(san);
                giaNgayThuong.setKhungGio(khungGio);
                giaNgayThuong.setLoai_ngay("NGAY_THUONG");
                giaNgayThuong.setGia(laKhungGioCao ? giaThuong.add(new BigDecimal("50000")) : giaThuong);
                giaSanRepository.save(giaNgayThuong);

                GiaSan giaCuoiTuanEntity = new GiaSan();
                giaCuoiTuanEntity.setSan(san);
                giaCuoiTuanEntity.setKhungGio(khungGio);
                giaCuoiTuanEntity.setLoai_ngay("CUOI_TUAN");
                giaCuoiTuanEntity.setGia(laKhungGioCao ? giaCuoiTuan.add(new BigDecimal("50000")) : giaCuoiTuan);
                giaSanRepository.save(giaCuoiTuanEntity);
            }
        }
    }

    private KhungGio taoKhungGio(LocalTime batDau, LocalTime ketThuc) {
        KhungGio khungGio = new KhungGio();
        khungGio.setGioBatDau(batDau);
        khungGio.setGioKetThuc(ketThuc);
        return khungGioRepository.save(khungGio);
    }
}
