package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.utils.enums.PaymentGatewayType;
import com.trainning.movie_booking_system.utils.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "payment_transactions",
        indexes = {
                @Index(name = "idx_transaction_id", columnList = "transaction_id"),
                @Index(name = "idx_booking_id", columnList = "booking_id"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_gateway_type", columnList = "gateway_type"),
                @Index(name = "idx_created_at_pt", columnList = "created_at")
        }
)
public class PaymentTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_type", length = 20, nullable = false)
    private PaymentGatewayType gatewayType; // VNPAY/...

    @Column(name = "transaction_id", length = 255, nullable = false, unique = true)
    private String transactionId; // vnp_TxnRef

    @Column(name = "gateway_order_id", length = 255)
    private String gatewayOrderId; // vnp_TransactionNo

    // Amounts
    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "discount_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal finalAmount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "VND";

    // Status & method
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PaymentStatus status;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // VISA/MASTERCARD/ATM...

    // Timestamps
    @Column(name = "initiated_at", nullable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    // Gateway payloads (JSON as String)
    @Column(name = "gateway_request", columnDefinition = "json")
    private String gatewayRequest;

    @Column(name = "gateway_response", columnDefinition = "json")
    private String gatewayResponse;

    // Security
    @Column(name = "request_signature", length = 512)
    private String requestSignature;

    @Column(name = "response_signature", length = 512)
    private String responseSignature;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    // Voucher (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    // Refund info
    @Column(name = "refund_id", length = 255)
    private String refundId;

    @Lob
    @Column(name = "refund_reason")
    private String refundReason;
}