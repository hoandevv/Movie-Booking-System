package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.config.VnPayProperties;
import com.trainning.movie_booking_system.dto.request.Payment.PaymentRequest;
import com.trainning.movie_booking_system.helper.VnPay.VnPayHelper;
import com.trainning.movie_booking_system.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VnPayServiceImpl implements VnPayService {

    private final VnPayProperties props;

    /**
     * Tạo payment URL cho VNPay gateway
     */
    @Override
    public String createPaymentUrl(String txnRef, long amountVnd, String orderInfo, String clientIp) {
        log.info("Creating Payment URL for TXN {}", txnRef);

        Map<String, String> vnp = new HashMap<>();
        vnp.put("vnp_Version", "2.1.0");
        vnp.put("vnp_Command", "pay");
        vnp.put("vnp_TmnCode", props.getTmnCode());
        vnp.put("vnp_Amount", String.valueOf(amountVnd * 100)); // x100
        vnp.put("vnp_CurrCode", "VND");
        vnp.put("vnp_TxnRef", txnRef);
        vnp.put("vnp_OrderInfo", orderInfo);
        vnp.put("vnp_OrderType", "other");
        vnp.put("vnp_Locale", "vn");
        vnp.put("vnp_ReturnUrl", props.getReturnUrl());
        vnp.put("vnp_IpAddr", clientIp);
        String now = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(java.time.LocalDateTime.now());
        vnp.put("vnp_CreateDate", now);

        // Ký dữ liệu
        Map<String, String> toSign = new HashMap<>(vnp);
        toSign.remove("vnp_Url");
        String hash = VnPayHelper.secureHash(props.getHashSecret(), toSign);
        toSign.put("vnp_SecureHash", hash);

        String query = VnPayHelper.buildQuery(toSign);
        return props.getPayUrl() + "?" + query;
    }

    /**
     * Verify signature từ callback VNPay
     */
    @Override
    public boolean verify(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isEmpty()) {
            log.warn("Missing vnp_SecureHash");
            return false;
        }
        Map<String, String> toSign = new HashMap<>(params);
        toSign.remove("vnp_SecureHash");
        toSign.remove("vnp_SecureHashType");

        String calculatedHash = VnPayHelper.secureHash(props.getHashSecret(), toSign);
        boolean isValid = receivedHash.equalsIgnoreCase(calculatedHash);
        if (!isValid) {
            log.warn("Invalid VNPay signature: expected {}, got {}", calculatedHash, receivedHash);
        }
        return isValid;
    }

    @Override
    public boolean verifySignature(PaymentRequest requestData) {
        if (requestData == null || requestData.getData() == null) return false;
        Map<String, String> params = new HashMap<>(requestData.getData());
        return verify(params);
    }

    @Override
    public boolean verifyPaymentCallbackSignature(PaymentRequest requestData) {
        return verifySignature(requestData);
    }

    /**
     * Parse callback từ VNPay (Return URL / Webhook)
     */
    @Override
    public PaymentRequest parseRequest(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String key = paramNames.nextElement();
            params.put(key, request.getParameter(key));
        }

        // Fix lỗi NumberFormatException: tách bookingId từ vnp_TxnRef
        String txnRef = params.get("vnp_TxnRef"); // e.g., "TXN_1763908409839_13"
        Long bookingId = null;
        try {
            if (txnRef != null && txnRef.contains("_")) {
                String[] parts = txnRef.split("_");
                bookingId = Long.parseLong(parts[2]); // phần cuối là bookingId
            }
        } catch (NumberFormatException e) {
            log.error("Failed to parse bookingId from vnp_TxnRef: {}", txnRef);
        }

        PaymentRequest pr = PaymentRequest.builder()
                .bookingId(bookingId)
                .transactionId(txnRef)
                .status("00".equals(params.get("vnp_TransactionStatus")) ? "SUCCESS" : "FAILED")
                .paymentMethod(params.get("vnp_BankCode"))
                .amount(params.get("vnp_Amount"))
                .transactionDate(params.get("vnp_PayDate"))
                .responseCode(params.get("vnp_ResponseCode"))
                .signature(params.get("vnp_SecureHash"))
                .extraData(params.toString())
                .data(params)
                .build();
        return pr;
    }

    /**
     * Simple return code for frontend demo
     * 1 = success, 0 = failed, -1 = invalid signature
     */
    @Override
    public int orderReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String key = params.nextElement();
            fields.put(key, request.getParameter(key));
        }

        String vnpSecureHash = fields.get("vnp_SecureHash");

        // Remove hash fields for verification
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        if (verify(fields)) {
            String transactionStatus = fields.get("vnp_TransactionStatus");
            return "00".equals(transactionStatus) ? 1 : 0;
        } else {
            return -1;
        }
    }
}
