package com.trainning.movie_booking_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "voucher_usages",
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_voucher_booking", columnNames = {"voucher_id", "booking_id"})
        },
        indexes = {
                @Index(name = "idx_voucher_id", columnList = "voucher_id"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_booking_id_vu", columnList = "booking_id"),
                @Index(name = "idx_used_at", columnList = "used_at")
        }
)
public class VoucherUsage extends com.trainning.movie_booking_system.entity.BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Account user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_transaction_id")
    private PaymentTransaction paymentTransaction;

    @Column(name = "original_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal originalAmount;

    @Column(name = "discount_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal finalAmount;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;
}
