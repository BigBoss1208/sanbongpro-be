// KhungGioRepository.java
package com.example.becicd.repository;

import com.example.becicd.entity.KhungGio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.Optional;

public interface KhungGioRepository extends JpaRepository<KhungGio, Long> {
    @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select k from KhungGio k where k.id = :id")
    Optional<KhungGio> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
	Optional<KhungGio> findByGioBatDauAndGioKetThuc(LocalTime gioBatDau, LocalTime gioKetThuc);
}
