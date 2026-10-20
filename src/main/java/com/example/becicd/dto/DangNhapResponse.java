package com.example.becicd.dto;

public class DangNhapResponse {
    private String token;
    private String username;
    private String role;
    private Long cumSanId;

    public DangNhapResponse(String token, String username, String role, Long cumSanId) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.cumSanId = cumSanId;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public Long getCumSanId() { return cumSanId; }
}