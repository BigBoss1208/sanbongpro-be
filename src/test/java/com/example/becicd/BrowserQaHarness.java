package com.example.becicd;
import com.example.becicd.entity.*;
import com.example.becicd.enums.VaiTro;
import com.example.becicd.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.nio.file.*;
import java.time.LocalTime;
import java.math.BigDecimal;

/** Opt-in HTTP browser fixture. Never uses a production database or production credentials.
 * mvn -Dtest=BrowserQaHarness -Dqa.browser=true test
 * Stop by creating target/qa-browser-stop. Excluded from the default *Test suite by name.
 */
@EnabledIfSystemProperty(named="qa.browser",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT,properties={
 "server.port=8085", "server.address=127.0.0.1", "spring.datasource.url=jdbc:h2:mem:browserqa;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "spring.datasource.driver-class-name=org.h2.Driver",
 "spring.datasource.username=sa", "spring.datasource.password=test-only", "spring.jpa.hibernate.ddl-auto=create-drop",
 "app.jwtSecret=isolated-browser-qa-signing-key-more-than-32-bytes", "app.cors-origins=http://127.0.0.1:4319,http://localhost:4319",
 "minio.endpoint=http://localhost:9000", "minio.access-key=test-only", "minio.secret-key=test-only", "minio.bucket=qa",
 "vnpay.tmn-code=QA", "vnpay.hash-secret=test-only", "vnpay.return-url=http://localhost/api/thanh-toan/vnpay/return"
})
class BrowserQaHarness {
 @Autowired NguoiDungRepository users; @Autowired SanRepository fields; @Autowired CumSanRepository clusters;
 @Autowired KhungGioRepository slots; @Autowired GiaSanRepository prices; @Autowired PasswordEncoder encoder;
 @Test void serveIsolatedBrowserQa() throws Exception {
  CumSan c=new CumSan(); c.setTenCum("QA — Cụm sân kiểm thử"); c=clusters.save(c);
  for(VaiTro role:VaiTro.values()) {
   String name="qa-"+role.name().toLowerCase();
   users.save(NguoiDung.builder().username(name).email(name+"@qa.invalid").hoTen(name).matKhau(encoder.encode("isolated-browser-password")).vaiTro(role).cumSan(role==VaiTro.NHAN_VIEN_PAGE||role==VaiTro.NHAN_VIEN_SAN?c:null).trangThai(true).build());
  }
  for(int hour:new int[]{7,12,18,20}) { KhungGio k=new KhungGio(); k.setGioBatDau(LocalTime.of(hour,0)); k.setGioKetThuc(LocalTime.of(hour+1,0)); slots.save(k); }
  for(int i=1;i<=4;i++) {
   San s=new San(); s.setTenSan("QA — Sân kiểm thử "+i); s.setLoaiSan(i%2==0?"Sân 7":"Sân 5"); s.setCumSan(c); s.setTrangThai("HOAT_DONG"); s.setMoTa("Dữ liệu QA cô lập, chỉ dùng để kiểm thử browser."); s=fields.save(s);
   for(KhungGio k:slots.findAll()) { GiaSan g=new GiaSan(); g.setSan(s); g.setKhungGio(k); g.setGia(BigDecimal.valueOf(200000+i*10000)); g.setLoai_ngay("TAT_CA"); prices.save(g); }
  }
  Path stop=Paths.get("target/qa-browser-stop"); Files.deleteIfExists(stop);
  Files.writeString(Paths.get("target/qa-browser-ready"),"http://127.0.0.1:8085");
  long end=System.currentTimeMillis()+1800000;
  while(!Files.exists(stop)&&System.currentTimeMillis()<end) Thread.sleep(1000);
 }
}
