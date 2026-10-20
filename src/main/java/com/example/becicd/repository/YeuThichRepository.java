package com.example.becicd.repository;

import com.example.becicd.entity.YeuThich;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface YeuThichRepository extends JpaRepository<YeuThich, Long> {
    List<YeuThich> findByNguoiDungIdOrderByIdDesc(Long nguoiDungId);
    Optional<YeuThich> findByNguoiDungIdAndSanId(Long nguoiDungId, Long sanId);
}