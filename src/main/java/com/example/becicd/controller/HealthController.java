package com.example.becicd.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final DataSource dataSource;
    public HealthController(DataSource dataSource) { this.dataSource = dataSource; }
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        boolean ready;
        try (Connection connection = dataSource.getConnection()) { ready = connection.isValid(2); }
        catch (Exception exception) { ready = false; }
        return ResponseEntity.status(ready ? 200 : 503).body(Map.of("status", ready ? "UP" : "DOWN", "timestamp", Instant.now().toString()));
    }
}
