package com.example.becicd.repository;

import com.example.becicd.entity.DatSan;
import com.example.becicd.enums.TrangThaiDatSan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DatSanRepository extends JpaRepository<DatSan, Long> {

    @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DatSan d where d.id = :id")
    java.util.Optional<DatSan> findLockedById(@Param("id") Long id);

    List<DatSan> findBySanIdAndNgayDat(Long sanId, LocalDate ngayDat);

    List<DatSan> findByKhachHangIdOrderByNgayTaoDesc(Long khachHangId);

    List<DatSan> findByTrangThaiOrderByNgayTaoDesc(TrangThaiDatSan trangThai);

    List<DatSan> findByKhachHangSoDienThoaiContainingOrderByNgayDatDesc(String soDienThoai);

    List<DatSan> findBySanIdAndKhungGioIdAndNgayDat(Long sanId, Long khungGioId, LocalDate ngayDat);

    long countByNgayDatBetween(LocalDate tuNgay, LocalDate denNgay);

    List<DatSan> findByNgayDatBetween(LocalDate tuNgay, LocalDate denNgay);

    @Query("select coalesce(sum(d.gia), 0) from DatSan d where d.ngayDat between :tuNgay and :denNgay and d.trangThai <> :trangThaiHuy")
    BigDecimal sumGiaByNgayDatBetweenAndTrangThaiNot(
            @Param("tuNgay") LocalDate tuNgay,
            @Param("denNgay") LocalDate denNgay,
            @Param("trangThaiHuy") TrangThaiDatSan trangThaiHuy
    );
}
