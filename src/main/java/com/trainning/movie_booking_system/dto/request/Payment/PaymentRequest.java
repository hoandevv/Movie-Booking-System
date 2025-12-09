package com.trainning.movie_booking_system.dto.request.Payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.Map;

/**
 * Payment Request DTO - Dữ liệu callback từ Payment Gateway
 * 
 * ⚠️ TODO: Customize theo gateway bạn chọn
 * =========================================
 * 
 * VNPay fields:
 * - vnp_TmnCode
 * - vnp_Amount
 * - vnp_BankCode
 * - vnp_BankTranNo
 * - vnp_CardType
 * - vnp_PayDate
 * - vnp_OrderInfo
 * - vnp_TransactionNo
 * - vnp_ResponseCode
 * - vnp_TransactionStatus
 * - vnp_TxnRef (bookingId)
 * - vnp_SecureHashType
 * - vnp_SecureHash
 * 
 * Stripe fields:
 * - id (payment intent ID)
 * - object
 * - amount
 * - currency
 * - status
 * - metadata.bookingId
 * 
 * MoMo fields:
 * - partnerCode
 * - orderId
 * - requestId
 * - amount
 * - orderInfo
 * - orderType
 * - transId
 * - resultCode
 * - message
 * - payType
 * - responseTime
 * - extraData
 * - signature
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PaymentRequest {
    
    /**
     * Booking ID to update
     */
    @NotNull(message = "Booking ID is required")
    @Positive(message = "Booking ID must be positive")
    private Long bookingId;
    
    /**
     * Transaction ID from gateway
     * Used for idempotency check
     */
    private String transactionId;
    
    /**
     * Payment status: SUCCESS, FAILED, PENDING, CANCELLED
     */
    @NotBlank(message = "Payment status is required")
    private String status;
    
    /**
     * Amount paid (in smallest currency unit, e.g., cents for USD, xu for VND)
     * VNPay: amount in xu (VND * 100)
     * Stripe: amount in cents
     */
    private String amount;
    
    /**
     * Payment gateway response code
     */
    private String responseCode;
    
    /**
     * Payment method used
     */
    private String paymentMethod;
    
    /**
     * Transaction date/time
     */
    private String transactionDate;
    
    /**
     * Signature/Hash from gateway for verification
     * CRITICAL: Must verify this before processing payment!
     */
    private String signature;
    
    /**
     * Optional message from gateway
     */
    private String message;
    
    /**
     * Extra data from gateway (JSON string)
     */
    private String extraData;
    /**
     * All raw data from gateway as key-value pairs
     */
    private Map<String,String> data;
    /**
     * When pare request
     * */
    private String gatewayType;
    /**
     *
     * */
    private String gatewayOrderId;
}