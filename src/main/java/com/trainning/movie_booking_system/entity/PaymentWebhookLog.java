package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.utils.enums.PaymentGatewayType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "payment_webhook_logs",
        indexes = {
                @Index(name = "idx_gateway_type_pwl", columnList = "gateway_type"),
                @Index(name = "idx_processed", columnList = "processed"),
                @Index(name = "idx_signature_valid", columnList = "signature_valid"),
                @Index(name = "idx_received_at", columnList = "received_at")
        }
)
public class PaymentWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_type", length = 20, nullable = false)
    private PaymentGatewayType gatewayType;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "request_headers", columnDefinition = "json")
    private String requestHeaders;

    @Column(name = "request_body", columnDefinition = "json")
    private String requestBody;

    @Column(name = "signature", length = 512)
    private String signature;

    @Column(name = "signature_valid")
    private Boolean signatureValid;

    @Column(name = "processed")
    private Boolean processed = Boolean.FALSE;

    @Lob
    @Column(name = "processing_error")
    private String processingError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_transaction_id")
    private PaymentTransaction paymentTransaction;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
