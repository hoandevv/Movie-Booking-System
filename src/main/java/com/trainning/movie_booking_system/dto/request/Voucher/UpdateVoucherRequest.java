package com.trainning.movie_booking_system.dto.request.Voucher;

import com.trainning.movie_booking_system.utils.enums.VoucherStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for updating an existing voucher (ADMIN only)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVoucherRequest {

    @Size(max = 255, message = "Voucher name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @DecimalMin(value = "0.0", message = "Minimum order amount must be 0 or greater")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.0", inclusive = false, message = "Maximum discount amount must be greater than 0")
    private BigDecimal maxDiscountAmount;

    @Min(value = 1, message = "Total usage limit must be at least 1")
    private Integer totalUsageLimit;

    @Min(value = 1, message = "Usage per user must be at least 1")
    private Integer usagePerUser;

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    private List<Long> applicableMovieIds;
    private List<Long> applicableTheaterIds;
    private List<Integer> applicableDaysOfWeek;
    private List<String> applicableTimeSlots;

    private VoucherStatus status;
    private Boolean isPublic;
}
