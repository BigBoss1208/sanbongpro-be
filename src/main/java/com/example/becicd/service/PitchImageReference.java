package com.example.becicd.service;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Validates external references only; never fetches user supplied URLs on the server. */
public final class PitchImageReference {
    private PitchImageReference() {}

    public static String httpsUrl(String value) {
        try {
            String text = value.trim();
            URI uri = URI.create(text);
            String host = uri.getHost();
            if (text.length() > 255 || uri.toASCIIString().length() > 255 || !"https".equalsIgnoreCase(uri.getScheme())
                    || host == null || !host.contains(".") || host.endsWith(".")
                    || host.matches("[0-9.]+") || host.contains(":")
                    || host.endsWith(".localhost") || host.endsWith(".local")
                    || host.endsWith(".internal") || uri.getUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getPath() == null || uri.getPath().length() < 2) {
                throw new IllegalArgumentException();
            }
            return "https" + uri.toASCIIString().substring(uri.getScheme().length());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "URL ảnh phải là HTTPS công khai, tối đa 255 ký tự, không chứa tài khoản, query hoặc fragment");
        }
    }

    public static String normalize(String value, MinioService storage) {
        if (value == null || value.isBlank()) return null;
        String text = value.trim();
        if (text.regionMatches(true, 0, "https://", 0, 8)) return httpsUrl(text);
        try { return storage.objectKey(text); }
        catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh sân không hợp lệ");
        }
    }
}
