package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Payment.PaymentRequest;
import com.trainning.movie_booking_system.dto.response.Payment.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service xử lý payment cho booking
 */
public interface PaymentService {

    /**
     * Tạo payment URL để redirect user sang Payment Gateway
     *
     * @param bookingId ID của booking cần thanh toán
     * @return Payment URL để redirect
     */
    String createPaymentUrl(Long bookingId);

    /**
     * Xử lý VNPay return callback
     *
     * @param request HttpServletRequest chứa VNPay params
     * @return Payment response với status
     */
    PaymentResponse handleVNPayReturn(HttpServletRequest request);

    /**
     * Xử lý callback từ Payment Gateway sau khi user thanh toán (DEPRECATED)
     *
     * @param request Payment callback data từ gateway
     * @return Payment response với trạng thái
     */
    @Deprecated
    PaymentResponse handlePaymentCallback(PaymentRequest request);

    /**
     * Verify payment status từ Payment Gateway
     *
     * @param bookingId ID của booking
     * @return Payment status
     */
    String verifyPaymentStatus(Long bookingId);

    /**
     * Cancel payment và update booking status
     *
     * @param bookingId ID của booking
     */
    void cancelPayment(Long bookingId);
}
