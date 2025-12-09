package com.trainning.movie_booking_system.dto.request.Voucher;

import com.trainning.movie_booking_system.dto.SeatInfo;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateVoucherRequest {

    @NotBlank(message = "Voucher code is required")
    @Size(max = 50, message = "Voucher code must not exceed 50 characters")
    private String voucherCode;

    @NotNull(message = "Booking ID is required")
    @Positive(message = "Booking ID must be positive")
    private Long bookingId;

    @NotNull(message = "Booking amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Booking amount must be greater than 0")
    private BigDecimal bookingAmount;
}
