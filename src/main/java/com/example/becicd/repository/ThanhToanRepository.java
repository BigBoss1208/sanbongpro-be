// ThanhToanRepository.java
package com.example.becicd.repository;

import com.example.becicd.entity.ThanhToan;
import com.example.becicd.enums.TrangThaiThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ThanhToanRepository extends JpaRepository<ThanhToan, Long> {
    java.util.Optional<ThanhToan> findByMaGiaoDichVnpay(String reference);
	List<ThanhToan> findByDatSanIdOrderByIdDesc(Long datSanId);
	List<ThanhToan> findByTrangThaiOrderByIdDesc(TrangThaiThanhToan trangThai);
	List<ThanhToan> findByTrangThaiAndThoiGianThanhToanBetweenOrderByThoiGianThanhToanAsc(
			TrangThaiThanhToan trangThai, LocalDateTime start, LocalDateTime end);
	    List<ThanhToan> findByTrangThaiAndThoiGianThanhToanGreaterThanEqualAndThoiGianThanhToanLessThanOrderByThoiGianThanhToanAsc(
		    TrangThaiThanhToan trangThai, LocalDateTime start, LocalDateTime end);
}
