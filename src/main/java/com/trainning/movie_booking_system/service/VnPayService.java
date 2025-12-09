package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Payment.PaymentRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface VnPayService {

    /**
     * Create a payment URL for VnPay gateway
     *
     * @param txnRef    Transaction reference
     * @param amountVnd Amount in VND
     * @param orderInfo Order information
     * @param clientIp  Client IP address
     * @return Payment URL as a String
     */
    String createPaymentUrl(String txnRef, long amountVnd, String orderInfo, String clientIp);

    /**
     * Verify the integrity of VnPay callback parameters
     *
     * @param params Map of callback parameters from VnPay
     * @return true if the signature is valid, false otherwise
     */
    boolean verify(Map<String, String> params);

    /**
     * Process VNPay return callback
     *
     * @param request HttpServletRequest containing VNPay callback params
     * @return 1 = success, 0 = failed, -1 = invalid signature
     */
    int orderReturn(HttpServletRequest request);


    /**
     * Parse HttpServletRequest VNPay callback thành PaymentRequest
     */
    PaymentRequest parseRequest(HttpServletRequest request);

    boolean verifyPaymentCallbackSignature(PaymentRequest requestData);

    /**
     * Verify signature of the PaymentRequest
     * Param request PaymentRequest object containing callback data
     * @return true if signature is valid, false otherwise
     * */
    boolean verifySignature(PaymentRequest request);
}
