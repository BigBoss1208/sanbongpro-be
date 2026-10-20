package com.example.becicd.controller;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class HealthControllerTest {
    @Test void databaseOutageReportsUnavailable() throws Exception {
        DataSource source = mock(DataSource.class); when(source.getConnection()).thenThrow(new SQLException("private connection info"));
        var result = new HealthController(source).health(); assertEquals(503, result.getStatusCodeValue()); assertEquals("DOWN", result.getBody().get("status")); assertFalse(result.getBody().toString().contains("private"));
    }
    @Test void validConnectionReportsUpAndCloses() throws Exception {
        DataSource source = mock(DataSource.class); Connection connection = mock(Connection.class); when(source.getConnection()).thenReturn(connection); when(connection.isValid(2)).thenReturn(true);
        assertEquals(200, new HealthController(source).health().getStatusCodeValue()); verify(connection).close();
    }
}
