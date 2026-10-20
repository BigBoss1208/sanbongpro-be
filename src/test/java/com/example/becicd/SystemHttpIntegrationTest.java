package com.example.becicd;

import com.example.becicd.entity.*;
import com.example.becicd.enums.*;
import com.example.becicd.repository.*;
import com.example.becicd.service.*;
import com.example.becicd.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:httpqa;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=test-only", "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwtSecret=isolated-http-test-signing-key-32-bytes-long", "app.cors-origins=http://localhost:4318",
    "minio.endpoint=http://localhost:9000", "minio.access-key=test-only", "minio.secret-key=test-only", "minio.bucket=qa",
    "vnpay.return-settlement-enabled=false", "vnpay.tmn-code=QA", "vnpay.hash-secret=test-only", "vnpay.return-url=http://localhost/api/thanh-toan/vnpay/return"
})
@AutoConfigureMockMvc
class SystemHttpIntegrationTest {
    @Autowired MockMvc http;
    @Autowired com.example.becicd.controller.VNPayController paymentController;
    @Autowired ObjectMapper json;
    @Autowired NguoiDungRepository users;
    @Autowired CumSanRepository clusters;
    @Autowired SanRepository fields;
    @Autowired KhungGioRepository slots;
    @Autowired GiaSanRepository prices;
    @Autowired DatSanRepository bookings;
    @Autowired ThanhToanRepository payments;
    @Autowired YeuThichRepository favorites;
    @Autowired JwtTokenProvider jwt;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactions;
    @MockBean MinioService storage;
    San field;
    KhungGio slot;
    NguoiDung customer, admin, page, onsite, outsider;
    LocalDate date;
    @BeforeEach void setup() {
        new TransactionTemplate(transactions).execute(status -> {
            favorites.deleteAllInBatch(); payments.deleteAllInBatch(); bookings.deleteAllInBatch(); prices.deleteAllInBatch();
            slots.deleteAllInBatch(); fields.deleteAllInBatch(); users.deleteAllInBatch(); clusters.deleteAllInBatch();
            CumSan cluster=new CumSan(); cluster.setTenCum("QA cluster"); cluster=clusters.save(cluster);
            CumSan other=new CumSan(); other.setTenCum("Other cluster"); other=clusters.save(other);
            customer=user("qa-customer",VaiTro.KHACH_HANG,null); admin=user("qa-admin",VaiTro.ADMIN,null);
            page=user("qa-page",VaiTro.NHAN_VIEN_PAGE,cluster); onsite=user("qa-onsite",VaiTro.NHAN_VIEN_SAN,cluster); outsider=user("qa-outsider",VaiTro.NHAN_VIEN_SAN,other);
            field=new San(); field.setTenSan("QA pitch"); field.setTrangThai("HOAT_DONG"); field.setCumSan(cluster); field=fields.save(field);
            LocalDateTime now=BookingGuard.now();
            // Near midnight use tomorrow at 00:01; the approved 30-minute window still applies.
            LocalDateTime start=now.plusMinutes(2).withSecond(0).withNano(0);
            if (start.toLocalTime().isAfter(LocalTime.of(23,30))) start=now.toLocalDate().plusDays(1).atTime(0,1);
            date=start.toLocalDate(); slot=new KhungGio(); slot.setGioBatDau(start.toLocalTime()); slot.setGioKetThuc(start.plusMinutes(20).toLocalTime()); slot=slots.save(slot);
            GiaSan price=new GiaSan(); price.setSan(field); price.setKhungGio(slot); price.setGia(BigDecimal.valueOf(250000)); prices.save(price);
            return null;
        });
    }
    NguoiDung user(String name,VaiTro role,CumSan cluster) { return users.save(NguoiDung.builder().username(name).email(name+"@qa.invalid").matKhau(encoder.encode("isolated-password")) .hoTen(name).soDienThoai("0901234567").vaiTro(role).cumSan(cluster).trangThai(true).build()); }
    String token(NguoiDung user) { return "Bearer "+jwt.generateToken(user.getUsername(),user.getVaiTro().name()); }
    String body(Object data) throws Exception { return json.writeValueAsString(data); }
    long book() throws Exception {
        String result=http.perform(post("/api/dat-san").header("Authorization",token(customer)).contentType("application/json").content(body(Map.of("sanId",field.getId(),"khungGioId",slot.getId(),"ngayDat",date.toString(),"phuongThucThanhToan","TIEN_MAT"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(result).get("id").asLong();
    }
    @Test void registrationLoginRoleAndExistingTokenLockAreEnforcedOverHttp() throws Exception {
        http.perform(post("/api/auth/dang-ky").contentType("application/json").content(body(Map.of("email","new@qa.invalid","password","isolated-password","hoTen","New user","soDienThoai","0901234567")))).andExpect(status().isCreated());
        String result=http.perform(post("/api/auth/dang-nhap").contentType("application/json").content(body(Map.of("username","new@qa.invalid","password","isolated-password")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String bearer="Bearer "+json.readTree(result).get("token").asText();
        http.perform(get("/api/nguoi-dung/me").header("Authorization",bearer)).andExpect(status().isOk()).andExpect(jsonPath("$.soDienThoai").value("0901234567"));
        http.perform(get("/api/admin/dashboard").header("Authorization",bearer)).andExpect(status().isForbidden());
        NguoiDung registered=users.findByEmail("new@qa.invalid").get(); registered.setTrangThai(false); users.save(registered);
        http.perform(get("/api/nguoi-dung/me").header("Authorization",bearer)).andExpect(status().isForbidden());
        http.perform(get("/api/nguoi-dung/me")).andExpect(status().isUnauthorized());
        http.perform(get("/api/thanh-toan/vnpay/return")).andExpect(status().isBadRequest());
    }
    @Test void cashWorkflowScopeHistoryInvoiceSnapshotAndDeletionSafety() throws Exception {
        http.perform(get("/api/san")).andExpect(status().isOk()).andExpect(jsonPath("$[0].tenSan").value("QA pitch"));
        long booking=book();
        http.perform(put("/api/nhan-vien/dat-san/"+booking+"/duyet").header("Authorization",token(customer))).andExpect(status().isForbidden());
        http.perform(get("/api/nhan-vien/dat-san/tim-kiem").param("q",String.valueOf(booking)).header("Authorization",token(outsider))).andExpect(status().isOk()).andExpect(content().json("[]"));
        http.perform(put("/api/nhan-vien/dat-san/"+booking+"/duyet").header("Authorization",token(page))).andExpect(status().isOk());
        long payment=payments.findByDatSanIdOrderByIdDesc(booking).get(0).getId();
        http.perform(put("/api/thanh-toan/"+payment+"/xac-nhan").header("Authorization",token(outsider))).andExpect(status().isForbidden());
        http.perform(put("/api/thanh-toan/"+payment+"/xac-nhan").header("Authorization",token(onsite))).andExpect(status().isOk()).andExpect(jsonPath("$.tongTien").value(250000));
        http.perform(put("/api/nhan-vien/dat-san/"+booking+"/checkin").header("Authorization",token(onsite))).andExpect(status().isOk()).andExpect(jsonPath("$.trangThai").value("DA_CHECKIN"));
        http.perform(get("/api/thanh-toan/dat-san/"+booking+"/hoa-don").header("Authorization",token(customer))).andExpect(status().isOk()).andExpect(jsonPath("$.gia").value(250000));
        new TransactionTemplate(transactions).execute(status -> { prices.findAll().get(0).setGia(BigDecimal.valueOf(900000)); return null; });
        http.perform(get("/api/nhan-vien/lich-ma-tran").param("ngay",date.toString()).header("Authorization",token(page)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].slots[0].gia").value(250000));
        http.perform(get("/api/admin/doanh-thu").param("tuNgay",BookingGuard.now().toLocalDate().toString()).param("denNgay",BookingGuard.now().toLocalDate().toString()).header("Authorization",token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.tongDoanhThu").value(250000));
        http.perform(delete("/api/admin/san/"+field.getId()).header("Authorization",token(admin))).andExpect(status().isConflict());
        http.perform(delete("/api/khung-gio/"+slot.getId()).header("Authorization",token(admin))).andExpect(status().isConflict());
        assertEquals(1,bookings.count()); assertEquals(1,payments.count());
    }
    @Test void cancellationPreventsCollectionAndFutureCheckin() throws Exception {
        date=date.plusDays(2); long id=book();
        http.perform(put("/api/nhan-vien/dat-san/"+id+"/duyet").header("Authorization",token(page))).andExpect(status().isOk());
        http.perform(put("/api/nhan-vien/dat-san/"+id+"/checkin").header("Authorization",token(onsite))).andExpect(status().isConflict());
        http.perform(put("/api/dat-san/"+id+"/huy").header("Authorization",token(customer))).andExpect(status().isOk());
        long payment=payments.findByDatSanIdOrderByIdDesc(id).get(0).getId();
        http.perform(put("/api/thanh-toan/"+payment+"/xac-nhan").header("Authorization",token(onsite))).andExpect(status().isConflict());
    }
    @Test void inactiveFieldsRemainVisibleOnlyInAdminListAndWalkinRejectsVnpay() throws Exception {
        new TransactionTemplate(transactions).execute(status -> { fields.findById(field.getId()).get().setTrangThai("TAM_NGUNG"); return null; });
        http.perform(get("/api/san")).andExpect(status().isOk()).andExpect(content().json("[]"));
        http.perform(get("/api/admin/san").header("Authorization",token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$[0].trangThai").value("TAM_NGUNG"));
        http.perform(get("/api/admin/san").header("Authorization",token(customer))).andExpect(status().isForbidden());
        http.perform(get("/api/san/"+field.getId()+"/image")).andExpect(status().isNotFound());
        new TransactionTemplate(transactions).execute(status -> { fields.findById(field.getId()).get().setTrangThai("HOAT_DONG"); return null; });
        Map<String,Object> request=new HashMap<>(); request.put("sanId",field.getId()); request.put("khungGioId",slot.getId()); request.put("ngayDat",date.toString()); request.put("tenKhachHang","Walkin QA"); request.put("soDienThoai","0912345678"); request.put("phuongThucThanhToan","VNPAY");
        http.perform(post("/api/nhan-vien/dat-san/walkin").header("Authorization",token(onsite)).contentType("application/json").content(body(request))).andExpect(status().isBadRequest());
        assertEquals(0,bookings.count());
        request.put("phuongThucThanhToan","TIEN_MAT");
        http.perform(post("/api/nhan-vien/dat-san/walkin").header("Authorization",token(onsite)).contentType("application/json").content(body(request))).andExpect(status().isCreated());
        ThanhToan payment=payments.findAll().get(0); assertEquals(payment.getTongTien(),payment.getSoTien()); assertEquals(TrangThaiThanhToan.THANH_CONG,payment.getTrangThai());
    }
    @Test void adminFieldCrudValidationCorsAndUploadContract() throws Exception {
        http.perform(get("/api/admin/dashboard").header("Authorization",token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.tongNhanVien").value(3));
        http.perform(options("/api/admin/san").header("Origin","http://localhost:4318").header("Access-Control-Request-Method","GET"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:4318"));
        http.perform(get("/api/admin/san")).andExpect(status().isUnauthorized());
        http.perform(post("/api/admin/san").header("Authorization",token(customer)).contentType("application/json").content(body(Map.of("tenSan","Denied")))).andExpect(status().isForbidden());
        http.perform(post("/api/admin/san").header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("tenSan","x".repeat(101))))).andExpect(status().isBadRequest());
        String result=http.perform(post("/api/admin/san").header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("tenSan"," New pitch ","loaiSan","Sân 7","cumSanId",field.getCumSan().getId()))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.tenSan").value("New pitch")).andReturn().getResponse().getContentAsString();
        long id=json.readTree(result).get("id").asLong();
        CumSan other=clusters.findAll().stream().filter(c -> !c.getId().equals(field.getCumSan().getId())).findFirst().get();
        http.perform(put("/api/admin/san/"+id).header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("tenSan","Updated pitch","cumSanId",other.getId()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.cumSanId").value(other.getId()));
        http.perform(put("/api/admin/san/"+id+"/trang-thai").param("trangThai","TAM_NGUNG").header("Authorization",token(admin))).andExpect(status().isOk());
        http.perform(get("/api/admin/san").header("Authorization",token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        http.perform(get("/api/san")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        org.springframework.mock.web.MockMultipartFile image=new org.springframework.mock.web.MockMultipartFile("file","qa.png","image/png",new byte[]{1,2,3});
        org.mockito.Mockito.when(storage.upload(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq("san"))).thenReturn("san/qa.png");
        org.mockito.Mockito.when(storage.getPublicUrl("san/qa.png")).thenReturn("https://qa.invalid/qa.png");
        http.perform(multipart("/api/admin/san/upload-anh").file(image).header("Authorization",token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.objectKey").value("san/qa.png"));
        http.perform(delete("/api/admin/san/"+id).header("Authorization",token(admin))).andExpect(status().isNoContent());
        assertFalse(fields.existsById(id)); assertTrue(fields.existsById(field.getId()));
    }
    @Test void scheduleCrudRejectsOverlapAndPreservesAdjacentSlots() throws Exception {
        new TransactionTemplate(transactions).execute(state -> { prices.deleteAllInBatch(); slots.deleteAllInBatch(); return null; });
        Map<String,String> range=Map.of("gioBatDau","08:00","gioKetThuc","09:00");
        http.perform(post("/api/khung-gio").header("Authorization",token(customer)).contentType("application/json").content(body(range))).andExpect(status().isForbidden());
        String response=http.perform(post("/api/khung-gio").header("Authorization",token(admin)).contentType("application/json").content(body(range))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id=json.readTree(response).get("id").asLong();
        http.perform(post("/api/khung-gio").header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("gioBatDau","08:30","gioKetThuc","09:30")))).andExpect(status().isConflict());
        http.perform(post("/api/khung-gio").header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("gioBatDau","09:00","gioKetThuc","10:00")))).andExpect(status().isOk());
        http.perform(put("/api/khung-gio/"+id).header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("gioBatDau","07:00","gioKetThuc","08:00")))).andExpect(status().isOk());
        http.perform(put("/api/khung-gio/"+id).header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("gioBatDau","09:30","gioKetThuc","10:30")))).andExpect(status().isConflict());
        http.perform(post("/api/khung-gio").header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("gioBatDau","11:00","gioKetThuc","11:00")))).andExpect(status().isBadRequest());
        http.perform(get("/api/khung-gio").header("Authorization",token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        http.perform(delete("/api/khung-gio/"+id).header("Authorization",token(admin))).andExpect(status().isNoContent());
    }
    @Test void priceCrudValidationDuplicateAndFieldFilter() throws Exception {
        Map<String,Object> payload=new HashMap<>(); payload.put("sanId",field.getId()); payload.put("khungGioId",slot.getId()); payload.put("gia",320000); payload.put("loaiNgay","NGAY_THUONG");
        String result=http.perform(post("/api/gia-san").header("Authorization",token(admin)).contentType("application/json").content(body(payload))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id=json.readTree(result).get("id").asLong();
        http.perform(post("/api/gia-san").header("Authorization",token(admin)).contentType("application/json").content(body(payload))).andExpect(status().isConflict());
        payload.put("gia",0); http.perform(put("/api/gia-san/"+id).header("Authorization",token(admin)).contentType("application/json").content(body(payload))).andExpect(status().isBadRequest());
        payload.put("gia",340000); http.perform(put("/api/gia-san/"+id).header("Authorization",token(admin)).contentType("application/json").content(body(payload))).andExpect(status().isOk()).andExpect(jsonPath("$.gia").value(340000));
        http.perform(get("/api/gia-san").param("sanId",String.valueOf(field.getId())).header("Authorization",token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        http.perform(delete("/api/gia-san/"+id).header("Authorization",token(customer))).andExpect(status().isForbidden());
        http.perform(delete("/api/gia-san/"+id).header("Authorization",token(admin))).andExpect(status().isNoContent());
    }
    @Test void favoritesAreIdempotentScopedAndProtected() throws Exception {
        String endpoint="/api/yeu-thich/"+field.getId();
        http.perform(post(endpoint)).andExpect(status().isUnauthorized());
        http.perform(post(endpoint).header("Authorization",token(admin))).andExpect(status().isForbidden());
        http.perform(post(endpoint).header("Authorization",token(customer))).andExpect(status().isOk());
        http.perform(post(endpoint).header("Authorization",token(customer))).andExpect(status().isOk());
        assertEquals(1,favorites.count());
        http.perform(get("/api/yeu-thich").header("Authorization",token(customer))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(field.getId()));
        NguoiDung another=user("qa-another",VaiTro.KHACH_HANG,null);
        http.perform(get("/api/yeu-thich").header("Authorization",token(another))).andExpect(status().isOk()).andExpect(content().json("[]"));
        http.perform(delete(endpoint).header("Authorization",token(another))).andExpect(status().isNoContent()); assertEquals(1,favorites.count());
        http.perform(delete(endpoint).header("Authorization",token(customer))).andExpect(status().isNoContent()); assertEquals(0,favorites.count());
    }
    @Test void manualTransferCollectionIsScopedAndIdempotent() throws Exception {
        String response=http.perform(post("/api/dat-san").header("Authorization",token(customer)).contentType("application/json").content(body(Map.of("sanId",field.getId(),"khungGioId",slot.getId(),"ngayDat",date.toString(),"phuongThucThanhToan","CHUYEN_KHOAN"))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id=json.readTree(response).get("id").asLong();
        http.perform(put("/api/nhan-vien/dat-san/"+id+"/duyet").header("Authorization",token(page))).andExpect(status().isOk());
        long payment=payments.findByDatSanIdOrderByIdDesc(id).get(0).getId();
        http.perform(get("/api/thanh-toan/quan-ly").header("Authorization",token(customer))).andExpect(status().isForbidden());
        http.perform(put("/api/thanh-toan/"+payment+"/xac-nhan").header("Authorization",token(onsite))).andExpect(status().isOk());
        http.perform(put("/api/thanh-toan/"+payment+"/xac-nhan").header("Authorization",token(onsite))).andExpect(status().isOk());
        assertEquals(1,payments.count()); assertEquals(TrangThaiThanhToan.THANH_CONG,payments.findById(payment).get().getTrangThai());
        http.perform(get("/api/thanh-toan/dat-san/"+id+"/hoa-don").header("Authorization",token(customer))).andExpect(status().isOk()).andExpect(jsonPath("$.gia").value(250000));
    }

    @Test void profileAndPasswordValidationPersistWithoutChangingRole() throws Exception {
        http.perform(put("/api/nguoi-dung/me").header("Authorization",token(customer)).contentType("application/json").content(body(Map.of("hoTen","Updated QA","email","updated@qa.invalid","soDienThoai","0912345678"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.hoTen").value("Updated QA")).andExpect(jsonPath("$.role").value("KHACH_HANG"));
        http.perform(get("/api/nguoi-dung/me").header("Authorization",token(customer))).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("updated@qa.invalid"));
        http.perform(put("/api/nguoi-dung/doi-mat-khau").header("Authorization",token(customer)).contentType("application/json").content(body(Map.of("matKhauCu","wrong-test-only","matKhauMoi","new-isolated-password")))).andExpect(status().isBadRequest());
        http.perform(put("/api/nguoi-dung/doi-mat-khau").header("Authorization",token(customer)).contentType("application/json").content(body(Map.of("matKhauCu","isolated-password","matKhauMoi","new-isolated-password")))).andExpect(status().isOk());
        http.perform(post("/api/auth/dang-nhap").contentType("application/json").content(body(Map.of("username",customer.getUsername(),"password","new-isolated-password")))).andExpect(status().isOk());
    }
    @Test void clusterStaffAndCustomerCrudAssignmentAndSafeDeletion() throws Exception {
        String response=http.perform(post("/api/admin/cum-san").header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("tenCum","QA created cluster"))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long cluster=json.readTree(response).get("id").asLong();
        http.perform(put("/api/admin/cum-san/"+cluster).header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("tenCum","QA updated cluster")))).andExpect(status().isOk());
        Map<String,String> person=Map.of("hoTen","QA staff","email","new-staff@qa.invalid","password","isolated-new-user","vaiTro","NHAN_VIEN_SAN");
        String staffResult=http.perform(post("/api/admin/nhan-vien").header("Authorization",token(admin)).contentType("application/json").content(body(person))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long staff=json.readTree(staffResult).get("id").asLong();
        http.perform(put("/api/admin/cum-san/nhan-vien/"+staff).header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("cumSanId",cluster)))).andExpect(status().isOk());
        http.perform(delete("/api/admin/cum-san/"+cluster).header("Authorization",token(admin))).andExpect(status().isConflict());
        http.perform(put("/api/admin/nhan-vien/"+staff).header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("hoTen","Updated staff","email","updated-staff@qa.invalid","vaiTro","NHAN_VIEN_PAGE")))).andExpect(status().isOk());
        http.perform(delete("/api/admin/nhan-vien/"+staff).header("Authorization",token(admin))).andExpect(status().isNoContent());
        http.perform(delete("/api/admin/cum-san/"+cluster).header("Authorization",token(admin))).andExpect(status().isNoContent());
        String customerResult=http.perform(post("/api/admin/khach-hang").header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("hoTen","QA new customer","email","new-customer@qa.invalid","password","isolated-new-user")))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long created=json.readTree(customerResult).get("id").asLong();
        http.perform(put("/api/admin/khach-hang/"+created).header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("hoTen","QA customer update","email","updated-customer@qa.invalid")))).andExpect(status().isOk());
        http.perform(put("/api/admin/nguoi-dung/"+created+"/khoa").header("Authorization",token(admin))).andExpect(status().isOk());
        http.perform(delete("/api/admin/khach-hang/"+created).header("Authorization",token(admin))).andExpect(status().isNoContent());
    }
    @Test void rejectingCashRequestCancelsPaymentAndKeepsHistory() throws Exception {
        long id=book();
        http.perform(put("/api/nhan-vien/dat-san/"+id+"/tu-choi").header("Authorization",token(page)).contentType("application/json").content(body(Map.of("lyDo","QA rejected")))).andExpect(status().isOk()).andExpect(jsonPath("$.trangThai").value("DA_HUY"));
        assertEquals(1,bookings.count()); assertEquals(1,payments.count()); assertEquals(TrangThaiThanhToan.THAT_BAI,payments.findAll().get(0).getTrangThai());
    }
    @Test void walkinManualTransferStartsPendingAndCanBeCollected() throws Exception {
        String result=http.perform(post("/api/nhan-vien/dat-san/walkin").header("Authorization",token(onsite)).contentType("application/json").content(body(Map.of("sanId",field.getId(),"khungGioId",slot.getId(),"ngayDat",date.toString(),"tenKhachHang","QA walkin transfer","soDienThoai","0912345678","phuongThucThanhToan","CHUYEN_KHOAN"))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id=json.readTree(result).get("id").asLong();
        ThanhToan payment=payments.findByDatSanIdOrderByIdDesc(id).get(0); assertEquals(TrangThaiThanhToan.CHO_THANH_TOAN,payment.getTrangThai());
        http.perform(put("/api/thanh-toan/"+payment.getId()+"/xac-nhan").header("Authorization",token(onsite))).andExpect(status().isOk());
    }

    @Test void vnPayReturnSupportsIpnOnlyAndLocalSettlementWithoutDoubleProcessing() throws Exception {
        String created = http.perform(post("/api/dat-san").header("Authorization", token(customer))
            .contentType("application/json").content(body(Map.of("sanId",field.getId(),"khungGioId",slot.getId(),"ngayDat",date.toString(),"phuongThucThanhToan","VNPAY"))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        http.perform(post("/api/thanh-toan/vnpay/create/" + id).header("Authorization",token(customer))).andExpect(status().isOk());
        String reference = payments.findByDatSanIdOrderByIdDesc(id).get(0).getMaGiaoDichVnpay();
        String amount = payments.findByDatSanIdOrderByIdDesc(id).get(0).getSoTien().multiply(BigDecimal.valueOf(100)).toBigInteger().toString();
        Map<String,String> data = new HashMap<>(Map.of("vnp_TmnCode","QA","vnp_TxnRef",reference,"vnp_Amount",amount,
            "vnp_ResponseCode","00","vnp_TransactionStatus","00","vnp_TransactionNo","123456"));
        String signed = com.example.becicd.util.VNPayUtil.buildQueryAndHash(data,"test-only");
        data.put("vnp_SecureHash", signed.substring(signed.indexOf("vnp_SecureHash=")+15));
        var request = get("/api/thanh-toan/vnpay/return"); data.forEach(request::param);
        http.perform(request).andExpect(status().isSeeOther()).andExpect(header().string("Location", "http://localhost:3004/ket-qua-thanh-toan?ref="+reference));
        http.perform(get("/api/thanh-toan/vnpay/status/"+reference).header("Authorization",token(customer)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VERIFYING"));
        org.springframework.test.util.ReflectionTestUtils.setField(paymentController, "returnSettlementEnabled", true);
        try { http.perform(request).andExpect(status().isSeeOther()); }
        finally { org.springframework.test.util.ReflectionTestUtils.setField(paymentController, "returnSettlementEnabled", false); }
        var ipn = get("/api/thanh-toan/vnpay/ipn"); data.forEach(ipn::param);
        http.perform(ipn).andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("02"));
        http.perform(ipn).andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("02"));
        http.perform(get("/api/thanh-toan/vnpay/status/"+reference).header("Authorization",token(customer)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
        http.perform(get("/api/thanh-toan/vnpay/ipn").param("vnp_TxnRef",reference,reference)).andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("97"));
    }
    @Test void historyReturnsOnlyAuthenticatedCustomerAndLatestPaymentStatus() throws Exception {
        String own = http.perform(post("/api/dat-san").header("Authorization",token(customer)).contentType("application/json")
            .content(body(Map.of("sanId",field.getId(),"khungGioId",slot.getId(),"ngayDat",date.toString(),"phuongThucThanhToan","TIEN_MAT"))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long ownId = json.readTree(own).get("id").asLong();
        NguoiDung otherCustomer = new TransactionTemplate(transactions).execute(status -> user("history-other",VaiTro.KHACH_HANG,null));
        http.perform(post("/api/dat-san").header("Authorization",token(otherCustomer)).contentType("application/json")
            .content(body(Map.of("sanId",field.getId(),"khungGioId",slot.getId(),"ngayDat",date.plusDays(1).toString(),"phuongThucThanhToan","TIEN_MAT"))))
            .andExpect(status().isCreated());
        http.perform(get("/api/dat-san/cua-toi").header("Authorization",token(customer)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(ownId)).andExpect(jsonPath("$[0].tenSan").value("QA pitch"))
            .andExpect(jsonPath("$[0].trangThaiThanhToan").value("CHO_THANH_TOAN"));
        http.perform(get("/api/dat-san/cua-toi")).andExpect(status().is4xxClientError());
    }
    @Test void rejectedImageUploadPreservesOldImageAndSuccessfulReplacementPersists() throws Exception {
        new TransactionTemplate(transactions).execute(state -> { fields.findById(field.getId()).get().setHinhAnh("san/old.png"); return null; });
        byte[] image = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aXs8AAAAASUVORK5CYII=");
        var file = new org.springframework.mock.web.MockMultipartFile("file","replacement.png","image/png",image);
        var rejected = org.mockito.Mockito.mock(io.minio.errors.ErrorResponseException.class);
        var detail = org.mockito.Mockito.mock(io.minio.messages.ErrorResponse.class);
        org.mockito.Mockito.when(detail.code()).thenReturn("SignatureDoesNotMatch");
        org.mockito.Mockito.when(rejected.errorResponse()).thenReturn(detail);
        org.mockito.Mockito.when(rejected.response()).thenReturn(new okhttp3.Response.Builder().request(new okhttp3.Request.Builder().url("http://storage.invalid").build()).protocol(okhttp3.Protocol.HTTP_1_1).code(403).message("Forbidden").build());
        org.mockito.Mockito.when(storage.upload(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq("san"))).thenThrow(rejected);
        http.perform(multipart("/api/admin/san/upload-anh").file(file).header("Authorization",token(admin)))
            .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("STORAGE_UPLOAD_REJECTED"));
        assertEquals("san/old.png",fields.findById(field.getId()).get().getHinhAnh());
        org.mockito.Mockito.when(storage.upload(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq("san"))).thenReturn("san/new.png");
        org.mockito.Mockito.when(storage.getPublicUrl("san/new.png")).thenReturn("http://storage.invalid/nhom2/san/new.png");
        org.mockito.Mockito.when(storage.objectKey("san/new.png")).thenReturn("san/new.png");
        http.perform(multipart("/api/admin/san/upload-anh").file(file).header("Authorization",token(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.objectKey").value("san/new.png"));
        assertEquals("san/old.png",fields.findById(field.getId()).get().getHinhAnh());
        http.perform(put("/api/admin/san/"+field.getId()).header("Authorization",token(admin)).contentType("application/json").content(body(Map.of("hinhAnh","san/new.png"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.hinhAnh").value("san/new.png"));
        assertEquals("san/new.png",fields.findById(field.getId()).get().getHinhAnh());
        http.perform(get("/api/san")).andExpect(status().isOk()).andExpect(jsonPath("$[0].hinhAnh").value("san/new.png"));
    }
}
