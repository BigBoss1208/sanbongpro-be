package com.example.becicd.controller;

import com.example.becicd.service.MinioService;
import com.example.becicd.enums.TrangThaiDatSan;
import com.example.becicd.enums.VaiTro;
import com.example.becicd.repository.DatSanRepository;
import com.example.becicd.repository.NguoiDungRepository;
import com.example.becicd.repository.ThanhToanRepository;
import com.example.becicd.entity.ThanhToan;
import com.example.becicd.enums.TrangThaiThanhToan;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final MinioService minioService;
    private final DatSanRepository datSanRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final ThanhToanRepository thanhToanRepository;

    public AdminDashboardController(MinioService minioService, DatSanRepository datSanRepository,
                                    NguoiDungRepository nguoiDungRepository, ThanhToanRepository thanhToanRepository) {
        this.minioService = minioService;
        this.datSanRepository = datSanRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.thanhToanRepository = thanhToanRepository;
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getDashboardStats() {
        LocalDate today = com.example.becicd.service.BookingGuard.now().toLocalDate();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        List<ThanhToan> paidThisWeek = giaoDichThanhCong(startOfWeek, endOfWeek);
        BigDecimal doanhThu = paidThisWeek.stream().map(ThanhToan::getSoTien).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("tongDoanhThuTuan", doanhThu != null ? doanhThu : BigDecimal.ZERO);
        stats.put("tongKhachHang", nguoiDungRepository.countByVaiTro(VaiTro.KHACH_HANG));
        stats.put("tongNhanVien", nguoiDungRepository.countByVaiTroIn(java.util.List.of(
            VaiTro.NHAN_VIEN_PAGE, VaiTro.NHAN_VIEN_SAN)));
        stats.put("tongLuotDat", datSanRepository.countByNgayDatBetween(startOfWeek, endOfWeek));

        List<Map<String, Object>> doanhThuTheoNgay = new ArrayList<>();
        Map<LocalDate, BigDecimal> doanhThuNgay = new LinkedHashMap<>();
        for (LocalDate date = startOfWeek; !date.isAfter(endOfWeek); date = date.plusDays(1)) {
            doanhThuNgay.put(date, BigDecimal.ZERO);
        }

        Map<String, BigDecimal> doanhThuTheoCum = new LinkedHashMap<>();
        paidThisWeek.forEach(payment -> themVaoThongKe(payment, doanhThuNgay, doanhThuTheoCum, null));

        doanhThuNgay.forEach((date, total) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("ngay", date.getDayOfWeek().getValue() == 7 ? "CN" : "T" + (date.getDayOfWeek().getValue() + 1));
            item.put("giaTri", total);
            doanhThuTheoNgay.add(item);
        });
        stats.put("doanhThuTheoNgay", doanhThuTheoNgay);
        stats.put("doanhThuTheoCum", doanhThuTheoCum);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/doanh-thu")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        if (tuNgay.isAfter(denNgay)) return ResponseEntity.badRequest().body(Map.of("message", "Ngày bắt đầu phải trước ngày kết thúc"));
        List<ThanhToan> paid = giaoDichThanhCong(tuNgay, denNgay);
        BigDecimal total = paid.stream().map(ThanhToan::getSoTien).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<LocalDate, BigDecimal> daily = new LinkedHashMap<>();
        Map<String, BigDecimal> byField = new LinkedHashMap<>();
        Map<String, BigDecimal> byCluster = new LinkedHashMap<>();
        for (LocalDate date = tuNgay; !date.isAfter(denNgay); date = date.plusDays(1)) daily.put(date, BigDecimal.ZERO);
        paid.forEach(payment -> themVaoThongKe(payment, daily, byCluster, byField));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tuNgay", tuNgay);
        result.put("denNgay", denNgay);
        result.put("tongDoanhThu", total);
        result.put("soGiaoDich", paid.size());
        result.put("doanhThuTheoNgay", daily.entrySet().stream().map(entry -> Map.of("ngay", entry.getKey(), "giaTri", entry.getValue())).collect(Collectors.toList()));
        result.put("doanhThuTheoSan", byField);
        result.put("doanhThuTheoCum", byCluster);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/bao-cao/excel")
    @Transactional(readOnly = true)
    public ResponseEntity<?> exportExcelReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BaoCaoDoanhThu");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Ngày");
            headerRow.createCell(1).setCellValue("Cụm Sân");
            headerRow.createCell(2).setCellValue("Doanh Thu (VNĐ)");

            if (tuNgay.isAfter(denNgay)) return ResponseEntity.badRequest().body(Map.of("message", "Ngày bắt đầu phải trước ngày kết thúc"));
            Row headers = sheet.getRow(0);
            headers.createCell(3).setCellValue("Sân");
            headers.createCell(4).setCellValue("Mã đơn");
            headers.createCell(5).setCellValue("Phương thức");
            int rowIndex = 1;
            for (ThanhToan payment : giaoDichThanhCong(tuNgay, denNgay)) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(payment.getThoiGianThanhToan().toLocalDate().toString());
                row.createCell(1).setCellValue(payment.getDatSan().getSan().getCumSan() == null ? "Chưa phân cụm" : payment.getDatSan().getSan().getCumSan().getTenCum());
                row.createCell(2).setCellValue(payment.getSoTien().doubleValue());
                row.createCell(3).setCellValue(payment.getDatSan().getSan().getTenSan());
                row.createCell(4).setCellValue(payment.getDatSan().getId());
                row.createCell(5).setCellValue(payment.getPhuongThuc().name());
            }

            workbook.write(out);

            MultipartFile multipartFile = new MockMultipartFile(
                    "file", "BaoCao_" + System.currentTimeMillis() + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );

            String fileUrl = minioService.uploadFile(multipartFile);
            return ResponseEntity.ok(Map.of("url", fileUrl, "message", "Xuất báo cáo Excel thành công!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Không thể tạo báo cáo Excel"));
        }
    }

    @GetMapping("/bao-cao/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<?> exportPdfReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        if (tuNgay.isAfter(denNgay)) return ResponseEntity.badRequest().body(Map.of("message", "Ngày bắt đầu phải trước ngày kết thúc"));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("BAO CAO DOANH THU"));
            document.add(new Paragraph("Tu ngay " + tuNgay + " den ngay " + denNgay));
            PdfPTable table = new PdfPTable(5);
            for (String header : new String[]{"Ngay", "Cum san", "San", "Ma don", "So tien (VND)"}) table.addCell(new Phrase(header));
            BigDecimal total = BigDecimal.ZERO;
            for (ThanhToan payment : giaoDichThanhCong(tuNgay, denNgay)) {
                table.addCell(payment.getThoiGianThanhToan().toLocalDate().toString());
                table.addCell(payment.getDatSan().getSan().getCumSan() == null ? "Chua phan cum" : payment.getDatSan().getSan().getCumSan().getTenCum());
                table.addCell(payment.getDatSan().getSan().getTenSan());
                table.addCell(String.valueOf(payment.getDatSan().getId()));
                table.addCell(payment.getSoTien().toPlainString());
                total = total.add(payment.getSoTien());
            }
            document.add(table);
            document.add(new Paragraph("Tong doanh thu: " + total.toPlainString() + " VND"));
            document.close();
            MultipartFile file = new MockMultipartFile("file", "BaoCao.pdf", "application/pdf", out.toByteArray());
            String fileUrl = minioService.uploadFile(file);
            return ResponseEntity.ok(Map.of("url", fileUrl, "message", "Xuất báo cáo PDF thành công"));
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Không thể tạo báo cáo PDF"));
        }
    }

    private List<ThanhToan> giaoDichThanhCong(LocalDate tuNgay, LocalDate denNgay) {
        return thanhToanRepository.findByTrangThaiAndThoiGianThanhToanGreaterThanEqualAndThoiGianThanhToanLessThanOrderByThoiGianThanhToanAsc(
                TrangThaiThanhToan.THANH_CONG, tuNgay.atStartOfDay(), denNgay.plusDays(1).atStartOfDay());
    }

    private void themVaoThongKe(ThanhToan payment, Map<LocalDate, BigDecimal> daily,
                                Map<String, BigDecimal> byCluster, Map<String, BigDecimal> byField) {
        if (payment.getThoiGianThanhToan() == null || payment.getSoTien() == null) return;
        LocalDate date = payment.getThoiGianThanhToan().toLocalDate();
        daily.computeIfPresent(date, (ignored, total) -> total.add(payment.getSoTien()));
        String cluster = payment.getDatSan().getSan().getCumSan() == null ? "Chưa phân cụm" : payment.getDatSan().getSan().getCumSan().getTenCum();
        byCluster.merge(cluster, payment.getSoTien(), BigDecimal::add);
        if (byField != null) byField.merge(payment.getDatSan().getSan().getTenSan(), payment.getSoTien(), BigDecimal::add);
    }
}
