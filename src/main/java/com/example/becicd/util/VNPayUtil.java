package com.example.becicd.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class VNPayUtil {

    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo chữ ký VNPay", e);
        }
    }

    public static String getIpAddress(javax.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-FORWARDED-FOR");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public static String getRandomTxnRef() {
        SecureRandom rnd = new SecureRandom();
        return String.valueOf(System.currentTimeMillis()) + rnd.nextInt(1000);
    }

    public static String buildQueryAndHash(Map<String, String> params, String hashSecret) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String,String> entry : sorted.entrySet()) {
            String key = entry.getKey(); String value = entry.getValue();
            if (value == null || value.isEmpty()) continue;
            if (!first) { hashData.append('&'); query.append('&'); }
            first = false;
            String encoded = URLEncoder.encode(value, StandardCharsets.US_ASCII);
            hashData.append(key).append('=').append(encoded);
            query.append(URLEncoder.encode(key, StandardCharsets.US_ASCII)).append('=').append(encoded);
        }
        String secureHash = hmacSHA512(hashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);
        return query.toString();
    }
}
