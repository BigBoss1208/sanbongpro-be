package com.example.becicd.controller;

import com.example.becicd.entity.CumSan;
import com.example.becicd.entity.NguoiDung;
import com.example.becicd.enums.VaiTro;
import com.example.becicd.repository.CumSanRepository;
import com.example.becicd.repository.NguoiDungRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/cum-san")
@PreAuthorize("hasRole('ADMIN')")
public class CumSanAdminController {

    private final CumSanRepository cumSanRepository;
    private final NguoiDungRepository nguoiDungRepository;

    public CumSanAdminController(CumSanRepository cumSanRepository, NguoiDungRepository nguoiDungRepository) {
        this.cumSanRepository = cumSanRepository;
        this.nguoiDungRepository = nguoiDungRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Map<String, Object>> danhSach() {
        return cumSanRepository.findAll().stream().map(this::toMap).collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> tao(@Valid @RequestBody CumSanRequest request) {
        CumSan cumSan = new CumSan();
        capNhat(cumSan, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(cumSanRepository.save(cumSan)));
    }

    @PutMapping("/{id}")
    public Map<String, Object> capNhat(@PathVariable Long id, @Valid @RequestBody CumSanRequest request) {
        CumSan cumSan = timCumSan(id);
        capNhat(cumSan, request);
        return toMap(cumSanRepository.save(cumSan));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> xoa(@PathVariable Long id) {
        CumSan cumSan = timCumSan(id);
        if (cumSan.getDanhSachSan() != null && !cumSan.getDanhSachSan().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chuyển sân sang cụm khác trước khi xóa cụm này");
        }
        if (!nguoiDungRepository.findByCumSanId(id).isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Chuyển nhân viên sang cụm khác trước khi xóa");
        cumSanRepository.delete(cumSan);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/nhan-vien/{staffId}")
    @Transactional
    public Map<String, Object> phanCongNhanVien(
            @PathVariable Long staffId,
            @RequestBody Map<String, Long> request) {
        NguoiDung staff = nguoiDungRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên"));
        if (staff.getVaiTro() != VaiTro.NHAN_VIEN_PAGE && staff.getVaiTro() != VaiTro.NHAN_VIEN_SAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ phân công tài khoản nhân viên");
        }
        Long cumSanId = request.get("cumSanId");
        staff.setCumSan(cumSanId == null ? null : timCumSan(cumSanId));
        nguoiDungRepository.save(staff);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("staffId", staff.getId());
        result.put("cumSan", staff.getCumSan() == null ? null : toMap(staff.getCumSan()));
        return result;
    }

    private void capNhat(CumSan cumSan, CumSanRequest request) {
        cumSan.setTenCum(request.getTenCum().trim());
        cumSan.setDiaChi(request.getDiaChi());
        cumSan.setMoTa(request.getMoTa());
    }

    private CumSan timCumSan(Long id) {
        return cumSanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cụm sân"));
    }

    private Map<String, Object> toMap(CumSan cumSan) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", cumSan.getId());
        result.put("tenCum", cumSan.getTenCum());
        result.put("diaChi", cumSan.getDiaChi());
        result.put("moTa", cumSan.getMoTa());
        result.put("soSan", cumSan.getDanhSachSan() == null ? 0 : cumSan.getDanhSachSan().size());
        return result;
    }

    public static class CumSanRequest {
        @NotBlank
        @Size(max = 100)
        private String tenCum;
        @Size(max = 255)
        private String diaChi;
        private String moTa;

        public String getTenCum() { return tenCum; }
        public void setTenCum(String tenCum) { this.tenCum = tenCum; }
        public String getDiaChi() { return diaChi; }
        public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
        public String getMoTa() { return moTa; }
        public void setMoTa(String moTa) { this.moTa = moTa; }
    }
}
