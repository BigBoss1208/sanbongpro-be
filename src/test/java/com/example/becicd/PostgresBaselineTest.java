package com.example.becicd;

import com.example.becicd.entity.*;
import com.example.becicd.enums.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: PostgreSQL baseline must already be applied on a disposable loopback DB. */
@EnabledIfSystemProperty(named="baseline.jdbc.url", matches="jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/qlsb_baseline_test_[a-z0-9]+")
class PostgresBaselineTest {
    @Test void realPostgresSchemaValidatesAndMappingsRoundTrip() throws Exception {
        String url = System.getProperty("baseline.jdbc.url");
        String user = System.getenv("BASELINE_TEST_USER");
        String password = System.getenv("BASELINE_TEST_PASSWORD");
        DriverManagerDataSource ds = new DriverManagerDataSource(url, user, password);
        try (Connection c = ds.getConnection()) {
            assertTrue(c.getCatalog().startsWith("qlsb_baseline_test_"));
            try (Statement s = c.createStatement()) {
                ResultSet r=s.executeQuery("select count(*) from pg_tables where schemaname='public'");
                assertTrue(r.next()); assertEquals(9,r.getInt(1));
                r=s.executeQuery("select count(*) from pg_constraint where contype='f' and connamespace='public'::regnamespace");
                assertTrue(r.next()); assertEquals(10,r.getInt(1));
                r=s.executeQuery("select count(*) from pg_index i join pg_class t on t.oid=i.indrelid where t.relnamespace='public'::regnamespace and i.indisunique");
                assertTrue(r.next()); assertEquals(14,r.getInt(1)); // 9 PK + 5 business uniqueness indexes
                r=s.executeQuery("select is_nullable,column_default from information_schema.columns where table_schema='public' and table_name='thanh_toan' and column_name='can_xu_ly_hoan_tien'");
                assertTrue(r.next()); assertEquals("NO",r.getString(1)); assertEquals("false",r.getString(2));
            }
            c.setAutoCommit(false);
            assertRejected(c,"insert into public.san(ten_san,cum_san_id) values ('FK test',-1)","23503");
            assertRejected(c,"insert into public.products(name,price,quantity) values (null,1,1)","23502");
            assertRejected(c,"insert into public.nguoi_dung(username,email,password,ho_ten,role,khoa_tai_khoan) values "
                    +"('duplicate','one@example.invalid','test-only-not-a-login','Test','ADMIN',false),"
                    +"('duplicate','two@example.invalid','test-only-not-a-login','Test','ADMIN',false)","23505");
            c.rollback();
        }
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(ds);
        factory.setPackagesToScan("com.example.becicd.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Map<String,Object> settings = new HashMap<>();
        settings.put("hibernate.hbm2ddl.auto","validate");
        settings.put("hibernate.dialect","org.hibernate.dialect.PostgreSQL10Dialect");
        settings.put("hibernate.physical_naming_strategy","org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy");
        settings.put("hibernate.implicit_naming_strategy","org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");
        factory.setJpaPropertyMap(settings);
        factory.afterPropertiesSet(); // Real Hibernate validate, never create/update.
        EntityManager em = Objects.requireNonNull(factory.getObject()).createEntityManager();
        try {
            assertEquals(9,em.getMetamodel().getEntities().size());
            try(Connection connection=ds.getConnection()) {
                for(javax.persistence.metamodel.EntityType<?> type:em.getMetamodel().getEntities()) {
                    assertEntityColumns(connection,type.getJavaType());
                }
            }
            em.getTransaction().begin();
            CumSan cluster=new CumSan(); cluster.setTenCum("Isolated validation cluster"); em.persist(cluster);
            NguoiDung account=new NguoiDung(); account.setUsername("baseline-test"); account.setEmail("baseline@example.invalid");
            account.setPassword(new BCryptPasswordEncoder().encode(UUID.randomUUID().toString()));
            String seedHashFile=System.getProperty("baseline.seed.hash.file");
            if(seedHashFile!=null) {
                java.nio.file.Path destination=java.nio.file.Path.of(seedHashFile).toAbsolutePath().normalize();
                assertTrue(destination.startsWith(java.nio.file.Path.of("target").toAbsolutePath().normalize()));
                java.nio.file.Files.writeString(destination,account.getPassword());
            }
            account.setHoTen("Isolated test"); account.setRole(VaiTro.ADMIN); account.setTrangThai(true); account.setCumSan(cluster); em.persist(account);
            San field=new San(); field.setTenSan("Isolated field"); field.setLoaiSan("Sân 5"); field.setTrangThai("HOAT_DONG"); field.setCumSan(cluster); em.persist(field);
            KhungGio slot=new KhungGio(); slot.setGioBatDau(LocalTime.of(17,0)); slot.setGioKetThuc(LocalTime.of(18,30)); em.persist(slot);
            GiaSan price=new GiaSan(); price.setSan(field); price.setKhungGio(slot); price.setGia(new BigDecimal("300000")); em.persist(price);
            DatSan booking=new DatSan(); booking.setKhachHang(account); booking.setSan(field); booking.setKhungGio(slot); booking.setNgayDat(LocalDate.now().plusDays(10)); booking.setGia(price.getGia()); em.persist(booking);
            ThanhToan payment=new ThanhToan(); payment.setDatSan(booking); payment.setSoTien(price.getGia()); payment.setTongTien(price.getGia()); payment.setPhuongThuc(PhuongThucThanhToan.VNPAY); payment.setTrangThai(TrangThaiThanhToan.CHO_THANH_TOAN); em.persist(payment);
            YeuThich favorite=new YeuThich(); favorite.setNguoiDung(account); favorite.setSan(field); em.persist(favorite);
            Product product=new Product("Validation", "isolated", 100.0, 1); em.persist(product); em.flush();
            assertEquals(Boolean.FALSE,em.createNativeQuery("select khoa_tai_khoan from nguoi_dung where id=:id").setParameter("id",account.getId()).getSingleResult());
            assertEquals(TrangThaiDatSan.GIU_CHO_TAM,booking.getTrangThai()); assertNotNull(booking.getNgayTao());
            assertEquals("TAT_CA",price.getLoai_ngay()); assertFalse(payment.isCanXuLyHoanTien());
            account.setTrangThai(false); em.flush();
            assertEquals(Boolean.TRUE,em.createNativeQuery("select khoa_tai_khoan from nguoi_dung where id=:id").setParameter("id",account.getId()).getSingleResult());
            for (TrangThaiDatSan state:TrangThaiDatSan.values()) { booking.setTrangThai(state); em.flush(); em.refresh(booking); assertEquals(state,booking.getTrangThai()); }
            for (TrangThaiThanhToan state:TrangThaiThanhToan.values()) { payment.setTrangThai(state); em.flush(); em.refresh(payment); assertEquals(state,payment.getTrangThai()); }
            for (PhuongThucThanhToan method:PhuongThucThanhToan.values()) { payment.setPhuongThuc(method); em.flush(); em.refresh(payment); assertEquals(method,payment.getPhuongThuc()); }
            for (VaiTro role:VaiTro.values()) { account.setRole(role); em.flush(); em.refresh(account); assertEquals(role,account.getRole()); }
            account.setTrangThai(true); em.flush(); em.clear();
            assertTrue(em.find(NguoiDung.class,account.getId()).isTrangThai());
        } finally {
            if(em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close(); factory.destroy();
        }
    }

    private static void assertRejected(Connection connection,String sql,String state) throws Exception {
        Savepoint point=connection.setSavepoint();
        try(Statement statement=connection.createStatement()) {
            SQLException error=assertThrows(SQLException.class,()->statement.executeUpdate(sql));
            assertEquals(state,error.getSQLState());
        } finally { connection.rollback(point); }
    }

    private static void assertEntityColumns(Connection connection,Class<?> type) throws Exception {
        String table=type.getAnnotation(javax.persistence.Table.class).name();
        Map<String,List<Object>> actual=new HashMap<>();
        try(PreparedStatement statement=connection.prepareStatement("select column_name,data_type,is_nullable,character_maximum_length,numeric_precision,numeric_scale,is_identity from information_schema.columns where table_schema='public' and table_name=?")) {
            statement.setString(1,table);
            try(ResultSet rows=statement.executeQuery()) {
                while(rows.next()) actual.put(rows.getString(1),Arrays.asList(rows.getString(2),rows.getString(3),rows.getObject(4),rows.getObject(5),rows.getObject(6),rows.getString(7)));
            }
        }
        Set<String> expected=new HashSet<>();
        for(java.lang.reflect.Field field:type.getDeclaredFields()) {
            if(java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.isAnnotationPresent(javax.persistence.OneToMany.class)) continue;
            javax.persistence.Column column=field.getAnnotation(javax.persistence.Column.class);
            javax.persistence.JoinColumn join=field.getAnnotation(javax.persistence.JoinColumn.class);
            boolean id=field.isAnnotationPresent(javax.persistence.Id.class);
            String name=join!=null?join.name():column!=null&&!column.name().isEmpty()?column.name():field.getName().replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase(java.util.Locale.ROOT);
            expected.add(name);
            List<Object> value=actual.get(name); assertNotNull(value,table+"."+name);
            Class<?> javaType=field.getType();
            String sqlType=join!=null||javaType==Long.class?"bigint":javaType==Boolean.class||javaType==boolean.class?"boolean":javaType==BigDecimal.class?"numeric":javaType==Double.class?"double precision":javaType==Integer.class?"integer":javaType==LocalDate.class?"date":javaType==LocalTime.class?"time without time zone":javaType==LocalDateTime.class?"timestamp without time zone":column!=null&&"TEXT".equalsIgnoreCase(column.columnDefinition())?"text":"character varying";
            assertEquals(sqlType,value.get(0),table+"."+name+" type");
            boolean nullable=!id && (join!=null?join.nullable():column==null||column.nullable());
            if(table.equals("thanh_toan")&&name.equals("can_xu_ly_hoan_tien")) nullable=false; // Explicit existing migration.
            assertEquals(nullable?"YES":"NO",value.get(1),table+"."+name+" nullability");
            if(sqlType.equals("character varying")) assertEquals(column==null?255:column.length(),((Number)value.get(2)).intValue(),table+"."+name+" length");
            if(sqlType.equals("numeric")) {
                assertEquals(column.precision(),((Number)value.get(3)).intValue());
                assertEquals(column.scale(),((Number)value.get(4)).intValue());
            }
            if(id) assertEquals("YES",value.get(5),table+" identity");
        }
        assertEquals(expected,actual.keySet(),table+" exact columns");
    }
}
