package com.example.becicd.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long thoiHanMs = 24 * 60 * 60 * 1000;

    public String taoToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + thoiHanMs))
                .signWith(key)
                .compact();
    }

    public String layUsername(String token) {
        return layClaims(token).getSubject();
    }

    public String layRole(String token) {
        return (String) layClaims(token).get("role");
    }

    public boolean hopLe(String token) {
        try {
            layClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims layClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }
}