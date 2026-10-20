package com.example.becicd.config;

import com.example.becicd.entity.San;
import com.example.becicd.repository.SanRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@org.springframework.core.annotation.Order(2)
@org.springframework.context.annotation.Profile("dev")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "demo.seed.enabled", havingValue = "true")
public class SanDataInitializer implements CommandLineRunner {

    private final SanRepository sanRepository;

    public SanDataInitializer(SanRepository sanRepository) {
        this.sanRepository = sanRepository;
    }

    @Override
    public void run(String... args) {
        Map<String, Integer> requiredByType = Map.of("Sân 5", 5, "Sân 7", 5, "Sân 11", 5);

        requiredByType.forEach((loaiSan, required) -> {
            long current = sanRepository.findAll().stream()
                    .filter(san -> loaiSan.equalsIgnoreCase(san.getLoaiSan()))
                    .count();

            for (int index = (int) current + 1; index <= required; index++) {
                San san = new San();
                san.setTenSan(loaiSan + " - " + String.format("%02d", index));
                san.setLoaiSan(loaiSan);
                san.setMoTa("Sân cỏ nhân tạo đạt chuẩn, sẵn sàng đặt lịch.");
                san.setTrangThai("HOAT_DONG");
                sanRepository.save(san);
            }
        });
    }
}
