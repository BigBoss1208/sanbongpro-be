package com.example.becicd.config;

import com.example.becicd.entity.GiaSan;
import com.example.becicd.entity.KhungGio;
import com.example.becicd.entity.San;
import com.example.becicd.repository.GiaSanRepository;
import com.example.becicd.repository.KhungGioRepository;
import com.example.becicd.repository.SanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;

@Component
@org.springframework.core.annotation.Order(3)
@org.springframework.context.annotation.Profile("dev")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "demo.seed.enabled", havingValue = "true")
public class ScheduleDataInitializer implements CommandLineRunner {

    private final KhungGioRepository khungGioRepository;
    private final GiaSanRepository giaSanRepository;
    private final SanRepository sanRepository;

    public ScheduleDataInitializer(KhungGioRepository khungGioRepository,
                                   GiaSanRepository giaSanRepository,
                                   SanRepository sanRepository) {
        this.khungGioRepository = khungGioRepository;
        this.giaSanRepository = giaSanRepository;
        this.sanRepository = sanRepository;
    }

    @Override
    public void run(String... args) {
        for (int startHour = 7; startHour <= 19; startHour += 2) {
            LocalTime start = LocalTime.of(startHour, 0);
            LocalTime end = LocalTime.of(startHour + 2, 0);
            KhungGio khungGio = khungGioRepository.findByGioBatDauAndGioKetThuc(start, end)
                    .orElseGet(() -> {
                        KhungGio created = new KhungGio();
                        created.setGioBatDau(start);
                        created.setGioKetThuc(end);
                        return khungGioRepository.save(created);
                    });

            for (San san : sanRepository.findAll()) {
                BigDecimal gia = giaTheoLoaiSan(san.getLoaiSan());
                if (gia == null || !giaSanRepository.findAllBySanIdAndKhungGioId(san.getId(), khungGio.getId()).isEmpty()) {
                    continue;
                }
                GiaSan giaSan = new GiaSan();
                giaSan.setSan(san);
                giaSan.setKhungGio(khungGio);
                giaSan.setGia(gia);
                giaSan.setLoai_ngay("TAT_CA");
                giaSanRepository.save(giaSan);
            }
        }
    }

    private BigDecimal giaTheoLoaiSan(String loaiSan) {
        if ("Sân 5".equalsIgnoreCase(loaiSan)) return new BigDecimal("200000");
        if ("Sân 7".equalsIgnoreCase(loaiSan)) return new BigDecimal("350000");
        if ("Sân 11".equalsIgnoreCase(loaiSan)) return new BigDecimal("600000");
        return null;
    }
}
