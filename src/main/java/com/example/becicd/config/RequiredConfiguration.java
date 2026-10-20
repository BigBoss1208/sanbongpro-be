package com.example.becicd.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import javax.annotation.PostConstruct;

@Configuration
public class RequiredConfiguration {
    private final Environment environment;
    public RequiredConfiguration(Environment environment) { this.environment = environment; }
    @PostConstruct
    public void validate() {
        for (String key : java.util.List.of("spring.datasource.url", "spring.datasource.username", "spring.datasource.password", "minio.endpoint", "minio.access-key", "minio.secret-key", "minio.bucket", "vnpay.tmn-code", "vnpay.hash-secret", "vnpay.pay-url", "vnpay.return-url")) {
            String value = environment.getProperty(key);
            if (value == null || value.isBlank()) throw new IllegalStateException("Missing required configuration: " + key);
        }
        for (String key : java.util.List.of("minio.endpoint", "vnpay.pay-url", "vnpay.return-url", "vnpay.frontend-result-url")) {
            java.net.URI uri = java.net.URI.create(environment.getProperty(key, "http://localhost:3004/ket-qua-thanh-toan"));
            if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) throw new IllegalStateException("Invalid URL configuration: " + key);
        }
    }
}
