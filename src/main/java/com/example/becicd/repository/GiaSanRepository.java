// GiaSanRepository.java
package com.example.becicd.repository;

import com.example.becicd.entity.GiaSan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GiaSanRepository extends JpaRepository<GiaSan, Long> {

    java.util.List<GiaSan> findAllBySanIdAndKhungGioId(Long sanId, Long khungGioId);

    java.util.List<GiaSan> findBySanIdOrderByKhungGioGioBatDauAsc(Long sanId);
}