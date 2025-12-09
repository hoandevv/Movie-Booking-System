package com.trainning.movie_booking_system.helper.VnPay;

import lombok.extern.slf4j.Slf4j;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class VnPayHelper {

    public static String hmacSHA512(String key, String data) {
        try {

            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return  sb.toString();
        } catch (Exception e) {
            log.warn("VnPayHelper - hmac512: " + e.getMessage());
            throw  new  RuntimeException("VnPayHelper - hmac512: " + e.getMessage());
        }
    }

    public static String buildQuery(Map<String, String> params) {
        // sort by key ASC
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            String v = params.get(k);
            if (v != null && !v.isEmpty()) {
                sb.append(k).append("=")
                        .append(URLEncoder.encode(v, StandardCharsets.UTF_8))
                        .append("&");
            }
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public static String secureHash(String hashSecret, Map<String, String> params) {
        // hash trên chuỗi **chưa** có vnp_SecureHash, đã sort & URL-encode value
        String data = buildQuery(params);
        return hmacSHA512(hashSecret, data);
    }

}
