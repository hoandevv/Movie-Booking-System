package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.utils.enums.DiscountType;
import com.trainning.movie_booking_system.utils.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "vouchers",
        indexes = {
                @Index(name = "idx_code", columnList = "code"),
                @Index(name = "idx_status_voucher", columnList = "status"),
                @Index(name = "idx_valid_period", columnList = "valid_from, valid_until"),
                @Index(name = "idx_is_public", columnList = "is_public")
        }
)
public class Voucher extends BaseEntity {

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20, nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    // BUY_X_GET_Y
    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    @Column(name = "get_quantity")
    private Integer getQuantity;

    // Usage limits
    @Column(name = "total_usage_limit")
    private Integer totalUsageLimit = 1000;

    @Column(name = "usage_per_user")
    private Integer usagePerUser = 1;

    @Column(name = "current_usage_count")
    private Integer currentUsageCount = 0;

    // Validity
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    // Scope (JSON)
    @Column(name = "applicable_movie_ids", columnDefinition = "json")
    private String applicableMovieIds;

    @Column(name = "applicable_theater_ids", columnDefinition = "json")
    private String applicableTheaterIds;

    @Column(name = "applicable_days_of_week", columnDefinition = "json")
    private String applicableDaysOfWeek;

    @Column(name = "applicable_time_slots", columnDefinition = "json")
    private String applicableTimeSlots;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private VoucherStatus status = VoucherStatus.ACTIVE;

    @Column(name = "is_public")
    private Boolean isPublic = Boolean.TRUE;

    // Người tạo voucher
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}