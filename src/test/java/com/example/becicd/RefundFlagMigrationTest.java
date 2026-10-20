package com.example.becicd;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

/** Core migration DDL tested only in isolated H2 PostgreSQL mode, never the real DB. */
class RefundFlagMigrationTest {
    @Test void addsBooleanDefaultPreservesRowsAndDoesNotResetFlagsOnRetry() throws Exception {
        String migration = Files.readString(Path.of("database/migrations/20261013_add_thanh_toan_refund_flag.sql"));
        // PostgreSQL transaction-local timeout commands are not supported by H2.
        String ddl = migration.substring(migration.indexOf("ALTER TABLE"), migration.indexOf("COMMIT;")).trim();
        try (Connection c = DriverManager.getConnection("jdbc:h2:mem:refund-migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE", "sa", "");
             Statement s = c.createStatement()) {
            s.execute("CREATE TABLE public.thanh_toan (id BIGINT PRIMARY KEY, tong_tien NUMERIC(19,2), ma_giao_dich_vnpay VARCHAR(100))");
            s.execute("INSERT INTO public.thanh_toan VALUES (1, 123456.75, 'qa-reference'), (2, 200000.00, NULL)");
            s.execute(ddl);
            try (ResultSet r = s.executeQuery("SELECT id, tong_tien, ma_giao_dich_vnpay, can_xu_ly_hoan_tien FROM public.thanh_toan ORDER BY id")) {
                assertTrue(r.next()); assertEquals(1, r.getLong(1));
                assertEquals(new java.math.BigDecimal("123456.75"), r.getBigDecimal(2));
                assertEquals("qa-reference", r.getString(3)); assertFalse(r.getBoolean(4)); assertFalse(r.wasNull());
                assertTrue(r.next()); assertEquals(2, r.getLong(1));
                assertEquals(new java.math.BigDecimal("200000.00"), r.getBigDecimal(2));
                assertNull(r.getString(3)); assertFalse(r.getBoolean(4)); assertFalse(r.wasNull());
                assertFalse(r.next());
            }
            s.execute("UPDATE public.thanh_toan SET can_xu_ly_hoan_tien=TRUE WHERE id=1");
            s.execute(ddl);
            try (ResultSet r = s.executeQuery("SELECT can_xu_ly_hoan_tien FROM public.thanh_toan WHERE id=1")) {
                assertTrue(r.next()); assertTrue(r.getBoolean(1));
            }
            assertThrows(SQLException.class, () -> s.execute("INSERT INTO public.thanh_toan(id, can_xu_ly_hoan_tien) VALUES(3, NULL)"));
        }
    }
}
