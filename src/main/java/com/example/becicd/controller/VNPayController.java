package com.example.becicd.controller;

import com.example.becicd.dto.VNPayResponseDTO;
import com.example.becicd.service.VNPayService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/thanh-toan/vnpay")
public class VNPayController {

    @org.springframework.beans.factory.annotation.Value("${vnpay.frontend-result-url:http://192.168.1.8:3004/ket-qua-thanh-toan}")
    private String frontendResultUrl;
    @org.springframework.beans.factory.annotation.Value("${vnpay.return-settlement-enabled:true}")
    private boolean returnSettlementEnabled;
    private final VNPayService vnPayService;

    public VNPayController(VNPayService vnPayService) {
        this.vnPayService = vnPayService;
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('KHACH_HANG')")
    @PostMapping("/create/{datSanId}")
    public ResponseEntity<VNPayResponseDTO> taoLinkThanhToan(@PathVariable Long datSanId,
                                                             HttpServletRequest request) {
        String url = vnPayService.createPaymentUrl(datSanId, request);
        return ResponseEntity.ok(new VNPayResponseDTO(url));
    }

    @GetMapping("/return")
    public ResponseEntity<String> xuLyKetQua(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        if (request.getParameterMap().values().stream().anyMatch(values -> values.length != 1)) return ResponseEntity.badRequest().body("Tham số thanh toán không hợp lệ");
        request.getParameterMap().forEach((key, value) -> params.put(key, value[0]));

        if (!vnPayService.validSignature(params)) return ResponseEntity.badRequest().body("Sai chữ ký thanh toán");
        String reference = params.get("vnp_TxnRef");
        if (reference == null || !reference.matches("[a-zA-Z0-9]{1,100}")) return ResponseEntity.badRequest().body("Mã giao dịch không hợp lệ");
        if (returnSettlementEnabled) vnPayService.handleReturn(params);
        return ResponseEntity.status(303).header("Location", frontendResultUrl + "?ref=" + reference).build();
    }

    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> ipn(HttpServletRequest request) {
        if (request.getParameterMap().values().stream().anyMatch(values -> values.length != 1))
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid parameters"));
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, values[0]));
        return ResponseEntity.ok(vnPayService.handleIpn(params));
    }

    @GetMapping("/status/{reference}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('KHACH_HANG')")
    public Map<String, Object> status(@PathVariable String reference) {
        return vnPayService.paymentStatus(reference);
    }
}
