package com.trainning.movie_booking_system.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration for VNPay payment gateway
 * Enables VnPayProperties and validates configuration on startup
 */
@Configuration
@EnableConfigurationProperties(VnPayProperties.class)
@RequiredArgsConstructor
@Slf4j
public class VNPayConfig {

    private final VnPayProperties vnPayProperties;

    /**
     * Validate VNPay configuration on application startup
     */
    @PostConstruct
    public void validateConfig() {
        log.info("Validating VNPay configuration...");
        
        if (vnPayProperties.getTmnCode() == null || vnPayProperties.getTmnCode().isEmpty()) {
            log.warn("VNPay TMN Code is not configured! Payment will not work.");
        }
        
        if (vnPayProperties.getHashSecret() == null || vnPayProperties.getHashSecret().isEmpty()) {
            log.warn("VNPay Hash Secret is not configured! Signature verification will fail.");
        }
        
        if (vnPayProperties.getPayUrl() == null || vnPayProperties.getPayUrl().isEmpty()) {
            log.warn("VNPay Payment URL is not configured!");
        }
        
        log.info("VNPay Config loaded:");
        log.info("   - TMN Code: {}", maskString(vnPayProperties.getTmnCode()));
        log.info("   - Hash Secret: {}", maskString(vnPayProperties.getHashSecret()));
        log.info("   - Pay URL: {}", vnPayProperties.getPayUrl());
        log.info("   - Return URL: {}", vnPayProperties.getReturnUrl());
        log.info("   - IPN URL: {}", vnPayProperties.getIpnUrl());
    }

    /**
     * Mask sensitive strings for logging (show first 4 chars only)
     */
    private String maskString(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 4) + "****";
    }
}
