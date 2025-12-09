package com.trainning.movie_booking_system.dto.response.Payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment Response DTO
 * Trả về cho frontend sau khi xử lý payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    /**
     * Booking ID đã thanh toán
     */
    private Long bookingId;
    
    /**
     * Transaction ID from payment gateway
     */
    private String transactionId;
    
    /**
     * Payment status: SUCCESS, FAILED, PENDING, CANCELLED
     */
    private String status;
    
    /**
     * Amount paid (formatted, e.g., "250,000 VND")
     */
    private String amount;
    
    /**
     * Payment method used (VNPAY, MOMO, STRIPE, etc.)
     */
    private String paymentMethod;
    
    /**
     * Message to display to user
     */
    private String message;
    
    /**
     * Payment URL (for createPayment response)
     * User will be redirected to this URL
     */
    private String paymentUrl;
}