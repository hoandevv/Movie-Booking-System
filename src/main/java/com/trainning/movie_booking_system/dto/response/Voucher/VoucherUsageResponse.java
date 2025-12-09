package com.trainning.movie_booking_system.dto.response.Voucher;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for voucher usage history
 * Shows when and how a user used a voucher
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherUsageResponse {

    private Long id;
    
    private Long voucherId;
    
    private String voucherCode;
    
    private String voucherName;
    
    private Long userId;
    
    private String userEmail;
    
    private Long bookingId;
    
    private BigDecimal originalAmount;
    
    private BigDecimal discountAmount;
    
    private BigDecimal finalAmount;
    
    private LocalDateTime usedAt;
    
    private String bookingStatus; // From Booking entity
}
