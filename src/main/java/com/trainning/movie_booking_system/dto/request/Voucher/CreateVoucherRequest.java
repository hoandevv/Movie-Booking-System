package com.trainning.movie_booking_system.dto.request.Voucher;

import com.trainning.movie_booking_system.utils.enums.DiscountType;
import com.trainning.movie_booking_system.utils.enums.VoucherStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for creating a new voucher (ADMIN only)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVoucherRequest {

    @NotBlank(message = "Voucher code is required")
    @Size(min = 3, max = 50, message = "Voucher code must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Voucher code must contain only uppercase letters, numbers, underscore and hyphen")
    private String code;

    @NotBlank(message = "Voucher name is required")
    @Size(max = 255, message = "Voucher name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    @NotNull(message = "Minimum order amount is required")
    @DecimalMin(value = "0.0", message = "Minimum order amount must be 0 or greater")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.0", inclusive = false, message = "Maximum discount amount must be greater than 0")
    private BigDecimal maxDiscountAmount;

    // For BUY_X_GET_Y type
    @Min(value = 1, message = "Buy quantity must be at least 1")
    private Integer buyQuantity;

    @Min(value = 1, message = "Get quantity must be at least 1")
    private Integer getQuantity;

    @NotNull(message = "Total usage limit is required")
    @Min(value = 1, message = "Total usage limit must be at least 1")
    private Integer totalUsageLimit;

    @NotNull(message = "Usage per user is required")
    @Min(value = 1, message = "Usage per user must be at least 1")
    private Integer usagePerUser;

    @NotNull(message = "Valid from date is required")
    private LocalDateTime validFrom;

    @NotNull(message = "Valid until date is required")
    private LocalDateTime validUntil;

    // Applicable scope (nullable = apply to all)
    private List<Long> applicableMovieIds;
    private List<Long> applicableTheaterIds;
    private List<Integer> applicableDaysOfWeek; // 1=Monday, 7=Sunday
    private List<String> applicableTimeSlots; // ["10:00-12:00", "18:00-22:00"]

    @NotNull(message = "Status is required")
    private VoucherStatus status;

    @NotNull(message = "Public flag is required")
    private Boolean isPublic;
}
