package com.trainning.movie_booking_system.dto.response.Voucher;

import com.trainning.movie_booking_system.utils.enums.DiscountType;
import com.trainning.movie_booking_system.utils.enums.VoucherStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for voucher information
 * Used in list vouchers and get voucher details
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherResponse {

    private Long id;
    
    private String code;
    
    private String name;
    
    private String description;
    
    private DiscountType discountType;
    
    private BigDecimal discountValue;
    
    private BigDecimal minOrderAmount;
    
    private BigDecimal maxDiscountAmount;
    
    // For BUY_X_GET_Y type
    private Integer buyQuantity;
    private Integer getQuantity;
    
    private Integer totalUsageLimit;
    
    private Integer currentUsageCount;
    
    private Integer usagePerUser;
    
    private LocalDateTime validFrom;
    
    private LocalDateTime validUntil;
    
    // Applicable scope
    private List<Long> applicableMovieIds;
    private List<Long> applicableTheaterIds;
    private List<Integer> applicableDaysOfWeek;
    private List<String> applicableTimeSlots;
    
    private VoucherStatus status;
    
    private Boolean isPublic;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Calculated field - can user still use this voucher?
    private Boolean canUse;
    
    // Remaining usage for current user (set when user is authenticated)
    private Integer userRemainingUsage;
}
