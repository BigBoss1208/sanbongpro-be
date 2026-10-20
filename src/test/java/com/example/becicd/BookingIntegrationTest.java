package com.example.becicd;

import com.example.becicd.controller.DatSanController;
import com.example.becicd.config.VNPayConfig;
import com.example.becicd.dto.TaoDatSanRequest;
import com.example.becicd.entity.*;
import com.example.becicd.enums.*;
import com.example.becicd.repository.*;
import com.example.becicd.security.CustomUserDetails;
import com.example.becicd.service.*;
import com.example.becicd.util.VNPayUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false", "vnpay.tmn-code=TEST", "vnpay.hash-secret=isolated-test-secret", "vnpay.return-url=http://localhost/api/thanh-toan/vnpay/return", "spring.datasource.url=jdbc:h2:mem:booking;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"})
@Import({BookingGuard.class, DatSanController.class, VNPayService.class, VNPayConfig.class, PaymentEmailService.class})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class BookingIntegrationTest {
    @Autowired SanRepository fields;
    @Autowired DatSanRepository bookings;
    @Autowired KhungGioRepository slots;
    @Autowired GiaSanRepository prices;
    @Autowired NguoiDungRepository users;
    @Autowired ThanhToanRepository payments;
    @Autowired BookingGuard guard;
    @Autowired DatSanController controller;
    @Autowired VNPayService vnpay;
    @Autowired VNPayConfig config;
    @Autowired PaymentEmailService emailService;
    @org.springframework.boot.test.mock.mockito.MockBean org.springframework.mail.javamail.JavaMailSender mailSender;
    @Autowired PlatformTransactionManager tx;
    NguoiDung customer;
    San field;
    KhungGio first, overlapping;
    LocalDate date;
    TransactionTemplate transaction;
    @BeforeEach void setup() {
        transaction = new TransactionTemplate(tx);
        transaction.execute(status -> {
            payments.deleteAllInBatch(); bookings.deleteAllInBatch(); prices.deleteAllInBatch(); slots.deleteAllInBatch(); fields.deleteAllInBatch(); users.deleteAllInBatch();
            customer = users.save(NguoiDung.builder().username("customer").email("customer@test.invalid").matKhau("test-password").hoTen("Customer").vaiTro(VaiTro.KHACH_HANG).trangThai(true).build());
            field = new San(); field.setTenSan("Test field"); field.setTrangThai("HOAT_DONG"); field = fields.save(field);
            first = slot(18, 19); overlapping = slot(18, 20);
            for (KhungGio slot : Arrays.asList(first, overlapping)) {
                GiaSan price = new GiaSan(); price.setSan(field); price.setKhungGio(slot); price.setGia(BigDecimal.valueOf(300000)); prices.save(price);
            }
            return null;
        });
        date = BookingGuard.now().toLocalDate().plusDays(1);
        ReflectionTestUtils.setField(config, "hashSecret", "isolated-test-secret");
        ReflectionTestUtils.setField(config, "tmnCode", "TEST");
        ReflectionTestUtils.setField(config, "payUrl", "https://sandbox.invalid/pay");
        ReflectionTestUtils.setField(config, "returnUrl", "http://localhost/api/thanh-toan/vnpay/return");
        ReflectionTestUtils.setField(emailService, "enabled", true);
        ReflectionTestUtils.setField(emailService, "from", "sender@test.invalid");
        ReflectionTestUtils.setField(emailService, "appPassword", "isolated-mail-test-password");
        authenticate();
    }
    KhungGio slot(int start, int end) { KhungGio slot=new KhungGio(); slot.setGioBatDau(LocalTime.of(start,0)); slot.setGioKetThuc(LocalTime.of(end,0)); return slots.save(slot); }
    void authenticate() { CustomUserDetails principal=new CustomUserDetails(customer); SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())); }
    TaoDatSanRequest request(KhungGio slot, PhuongThucThanhToan method) { TaoDatSanRequest request=new TaoDatSanRequest(); request.setSanId(field.getId()); request.setKhungGioId(slot.getId()); request.setNgayDat(date); request.setPhuongThucThanhToan(method); return request; }
    long book(KhungGio slot, PhuongThucThanhToan method) { return controller.taoDatSan(request(slot, method)).getBody().getId(); }
    @AfterEach void clearAuth() { SecurityContextHolder.clearContext(); }
    @Test void overlapsAndAdjacentAreDistinguished() { assertTrue(BookingGuard.overlaps(first, overlapping)); KhungGio next=new KhungGio(); next.setGioBatDau(LocalTime.of(19,0)); next.setGioKetThuc(LocalTime.of(20,0)); assertFalse(BookingGuard.overlaps(first,next)); }
    @Test void createsAmountsAndRejectsPastInactiveAndOverlap() {
        long id=book(first,PhuongThucThanhToan.TIEN_MAT);
        transaction.execute(status -> { ThanhToan payment=payments.findByDatSanIdOrderByIdDesc(id).get(0); assertEquals(0,payment.getTongTien().compareTo(payment.getSoTien())); return null; });
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> book(overlapping,PhuongThucThanhToan.TIEN_MAT));
        date=BookingGuard.now().toLocalDate().minusDays(1);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> book(first,PhuongThucThanhToan.TIEN_MAT));
        date=date.plusDays(3);
        transaction.execute(status -> { San stored=fields.findById(field.getId()).get(); stored.setTrangThai("TAM_NGUNG"); return null; });
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> book(first,PhuongThucThanhToan.TIEN_MAT));
    }
    @Test void concurrentDifferentSlotIdsCannotOverlap() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch gate=new CountDownLatch(1);
        try {
            Callable<Boolean> a=()->{ authenticate(); gate.await(); try { book(first,PhuongThucThanhToan.TIEN_MAT); return true; } catch (org.springframework.web.server.ResponseStatusException ex) { assertEquals(409,ex.getRawStatusCode()); return false; } finally { clearAuth(); } };
            Callable<Boolean> b=()->{ authenticate(); gate.await(); try { book(overlapping,PhuongThucThanhToan.TIEN_MAT); return true; } catch (org.springframework.web.server.ResponseStatusException ex) { assertEquals(409,ex.getRawStatusCode()); return false; } finally { clearAuth(); } };
            Future<Boolean> x=pool.submit(a), y=pool.submit(b); gate.countDown(); assertNotEquals(x.get(15,TimeUnit.SECONDS),y.get(15,TimeUnit.SECONDS)); assertEquals(1,bookings.count());
        } finally { pool.shutdownNow(); }
    }
    @Test void expiredHoldReleasesOverlapAndLateCallbackCannotRevive() {
        long old=book(first,PhuongThucThanhToan.VNPAY); vnpay.createPaymentUrl(old,new MockHttpServletRequest());
        transaction.execute(status -> { bookings.findById(old).get().setThoiGianHetHanGiuCho(BookingGuard.now().minusSeconds(1)); return null; });
        long replacement=book(overlapping,PhuongThucThanhToan.TIEN_MAT);
        assertFalse(vnpay.handleReturn(callback(old)));
        org.mockito.Mockito.verifyNoInteractions(mailSender);
        transaction.execute(status -> { assertEquals(TrangThaiDatSan.GIU_CHO_TAM,bookings.findById(old).get().getTrangThai()); assertTrue(payments.findByDatSanIdOrderByIdDesc(old).get(0).isCanXuLyHoanTien()); assertEquals(TrangThaiDatSan.CHO_XAC_NHAN,bookings.findById(replacement).get().getTrangThai()); return null; });
    }
    @Test void signedCallbacksAreIdempotentAndValidateAmountSignatureAndOwner() {
        long id=book(first,PhuongThucThanhToan.VNPAY); String initial=vnpay.createPaymentUrl(id,new MockHttpServletRequest()); String retry=vnpay.createPaymentUrl(id,new MockHttpServletRequest()); assertEquals(1,payments.count());
    Map<String,String> callback=callback(id); callback.put("vnp_SecureHash","invalid"); assertFalse(vnpay.handleReturn(callback));
        callback=callback(id); callback.put("vnp_Amount","1"); sign(callback); assertFalse(vnpay.handleReturn(callback));
        callback=callback(id); assertTrue(vnpay.handleReturn(callback)); assertTrue(vnpay.handleReturn(callback));
        transaction.execute(status -> { assertEquals(TrangThaiDatSan.DA_XAC_NHAN,bookings.findById(id).get().getTrangThai()); assertEquals(TrangThaiThanhToan.THANH_CONG,payments.findByDatSanIdOrderByIdDesc(id).get(0).getTrangThai()); return null; });
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->vnpay.createPaymentUrl(id,new MockHttpServletRequest()));
    }
        @Test void ownerAndPaidCancellationAndRollback() {
        long id=book(first,PhuongThucThanhToan.VNPAY);
        NguoiDung owner=customer;
        customer=NguoiDung.builder().id(999L).vaiTro(VaiTro.KHACH_HANG).build(); authenticate();
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->vnpay.createPaymentUrl(id,new MockHttpServletRequest()));
        customer=owner; authenticate(); vnpay.createPaymentUrl(id,new MockHttpServletRequest()); assertTrue(vnpay.handleReturn(callback(id))); controller.huyDatSan(id);
        transaction.execute(status->{ assertEquals(TrangThaiThanhToan.THANH_CONG,payments.findByDatSanIdOrderByIdDesc(id).get(0).getTrangThai()); assertTrue(payments.findByDatSanIdOrderByIdDesc(id).get(0).isCanXuLyHoanTien()); return null; });
        long count=bookings.count(); date=date.plusDays(1);
        assertThrows(RuntimeException.class,()->transaction.execute(status->{ book(first,PhuongThucThanhToan.TIEN_MAT); throw new RuntimeException("simulated persistence failure"); }));
        assertEquals(count,bookings.count()); assertEquals(1,payments.count());
    }
    @Test void cancellationBlocksCallbackAndMarksPaidRefundReview() {
        long id=book(first,PhuongThucThanhToan.VNPAY); vnpay.createPaymentUrl(id,new MockHttpServletRequest()); Map<String,String> callback=callback(id); controller.huyDatSan(id); assertFalse(vnpay.handleReturn(callback));
        org.mockito.Mockito.verifyNoInteractions(mailSender);
        transaction.execute(status->{assertEquals(TrangThaiDatSan.DA_HUY,bookings.findById(id).get().getTrangThai()); assertTrue(payments.findByDatSanIdOrderByIdDesc(id).get(0).isCanXuLyHoanTien()); return null;});
    }
    @Test void ipnValidatesAcknowledgementsAndStatusOwnership() {
        long id = book(first, PhuongThucThanhToan.VNPAY);
        vnpay.createPaymentUrl(id, new MockHttpServletRequest());
        Map<String,String> data = callback(id);
        String reference = data.get("vnp_TxnRef");
        assertEquals("VERIFYING", vnpay.paymentStatus(reference).get("status"));
        data.put("vnp_SecureHash", "bad");
        assertEquals("97", vnpay.handleIpn(data).get("RspCode"));
        data = callback(id); data.put("vnp_Amount", "1"); sign(data);
        assertEquals("04", vnpay.handleIpn(data).get("RspCode"));
        data = callback(id);
        assertTrue(vnpay.validSignature(data));
        assertEquals("VERIFYING", vnpay.paymentStatus(reference).get("status"));
        assertEquals("00", vnpay.handleIpn(data).get("RspCode"));
        assertEquals("02", vnpay.handleIpn(data).get("RspCode"));
        assertEquals("SUCCESS", vnpay.paymentStatus(reference).get("status"));
        customer = NguoiDung.builder().id(999L).vaiTro(VaiTro.KHACH_HANG).build(); authenticate();
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> vnpay.paymentStatus(reference));
    }
    @Test void failedIpnNeverConfirmsAndLateSuccessNeedsRefundReview() {
        long id = book(first, PhuongThucThanhToan.VNPAY);
        vnpay.createPaymentUrl(id, new MockHttpServletRequest());
        Map<String,String> data = callback(id); String reference = data.get("vnp_TxnRef");
        data.put("vnp_ResponseCode", "24"); data.put("vnp_TransactionStatus", "02"); sign(data);
        assertEquals("00", vnpay.handleIpn(data).get("RspCode"));
        assertEquals("FAILED", vnpay.paymentStatus(reference).get("status"));
        assertEquals("02", vnpay.handleIpn(data).get("RspCode"));
        assertEquals("00", vnpay.handleIpn(callback(id)).get("RspCode"));
        assertEquals("REFUND_REVIEW", vnpay.paymentStatus(reference).get("status"));
        assertEquals("02", vnpay.handleIpn(callback(id)).get("RspCode"));
    }
    @Test void emailIsSentOnceForVerifiedReturnAndDuplicateIpn() {
        long id = book(first, PhuongThucThanhToan.VNPAY);
        vnpay.createPaymentUrl(id, new MockHttpServletRequest());
        Map<String,String> data = callback(id);
        data.put("vnp_SecureHash", "invalid");
        assertFalse(vnpay.handleReturn(data));
        org.mockito.Mockito.verifyNoInteractions(mailSender);
        data = callback(id);
        assertTrue(vnpay.handleReturn(data));
        assertEquals("02", vnpay.handleIpn(data).get("RspCode"));
        assertTrue(vnpay.handleReturn(data));
        var captured = org.mockito.ArgumentCaptor.forClass(org.springframework.mail.SimpleMailMessage.class);
        org.mockito.Mockito.verify(mailSender, org.mockito.Mockito.times(1)).send(captured.capture());
        assertArrayEquals(new String[]{customer.getEmail()}, captured.getValue().getTo());
        assertTrue(captured.getValue().getText().contains("Customer"));
        assertTrue(captured.getValue().getText().contains("Test field"));
        assertTrue(captured.getValue().getText().contains("18:00 - 19:00"));
        assertTrue(captured.getValue().getText().contains("300.000"));
        assertTrue(captured.getValue().getText().contains("thành công"));
    }
    @Test void rollbackDoesNotSendEmailAndSmtpFailureDoesNotRollbackPayment() {
        long id = book(first, PhuongThucThanhToan.VNPAY);
        vnpay.createPaymentUrl(id, new MockHttpServletRequest());
        Map<String,String> data = callback(id);
        transaction.execute(status -> { assertEquals("00", vnpay.handleIpn(data).get("RspCode")); status.setRollbackOnly(); return null; });
        org.mockito.Mockito.verifyNoInteractions(mailSender);
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("isolated SMTP test failure"))
            .when(mailSender).send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
        assertEquals("00", vnpay.handleIpn(data).get("RspCode"));
        assertEquals("SUCCESS", vnpay.paymentStatus(data.get("vnp_TxnRef")).get("status"));
        assertEquals("02", vnpay.handleIpn(data).get("RspCode"));
        org.mockito.Mockito.verify(mailSender, org.mockito.Mockito.times(1)).send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
    }
    Map<String,String> callback(long id) {
        String reference=transaction.execute(status -> payments.findByDatSanIdOrderByIdDesc(id).get(0).getMaGiaoDichVnpay());
        Map<String,String> data=new HashMap<>(); data.put("vnp_TxnRef",reference); data.put("vnp_TmnCode","TEST"); data.put("vnp_Amount","30000000"); data.put("vnp_ResponseCode","00"); data.put("vnp_TransactionStatus","00"); data.put("vnp_TransactionNo","123456"); sign(data); return data;
    }
    void sign(Map<String,String> data) { data.remove("vnp_SecureHash"); String query=VNPayUtil.buildQueryAndHash(data,"isolated-test-secret"); data.put("vnp_SecureHash",query.substring(query.indexOf("vnp_SecureHash=")+15)); }
}
