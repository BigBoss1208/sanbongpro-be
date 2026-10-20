package com.example.becicd.repository;

import com.example.becicd.entity.NguoiDung;
import com.example.becicd.enums.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, Long> {

    Optional<NguoiDung> findByEmail(String email);

    Boolean existsByEmail(String email);
    Boolean existsByEmailIgnoreCase(String email);

    Boolean existsByUsername(String username);

    // Tìm người dùng theo 1 vai trò
    List<NguoiDung> findByVaiTro(VaiTro vaiTro);

    long countByVaiTro(VaiTro vaiTro);

    // Tìm người dùng theo danh sách vai trò
    List<NguoiDung> findByVaiTroIn(Collection<VaiTro> vaiTros);

    long countByVaiTroIn(Collection<VaiTro> vaiTros);

    Optional<NguoiDung> findByUsername(String username);

    Optional<NguoiDung> findByUsernameOrEmail(String username, String email);

    Optional<NguoiDung> findFirstBySoDienThoaiAndVaiTroOrderByIdAsc(String soDienThoai, VaiTro vaiTro);

    List<NguoiDung> findByCumSanId(Long cumSanId);
}
