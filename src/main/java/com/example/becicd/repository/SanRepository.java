package com.example.becicd.repository;

import com.example.becicd.entity.San;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SanRepository extends JpaRepository<San, Long> {

    @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from San s where s.id = :id")
    java.util.Optional<San> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);

    List<San> findByCumSanId(Long cumSanId);
}
