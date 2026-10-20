package com.example.becicd.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs:86400000}")
    private int jwtExpirationMs;

    @javax.annotation.PostConstruct
    public void validateConfiguration() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is missing or blank. For local startup, initialize .env.local and activate the dev or smoke profile.");
        }
        int secretBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (secretBytes < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes; configured length is " + secretBytes + " bytes.");
        }
        if (jwtExpirationMs <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION_MS must be positive; configured value is " + jwtExpirationMs + ".");
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // 1. Tạo JWT Token từ Username & Role
    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("vaiTro", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 2. Lấy Username từ JWT Token
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    // 3. Lấy Role từ JWT Token
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        Object roleObj = claims.get("role");
        if (roleObj == null) {
            roleObj = claims.get("vaiTro");
        }
        return roleObj != null ? roleObj.toString() : null;
    }

    // 4. Validate Token
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
