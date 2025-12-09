package com.trainning.movie_booking_system.dto.request.Payment;

import com.trainning.movie_booking_system.utils.enums.PaymentGatewayType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentRequest {
    @NotNull(message = "Booking ID must not be null")
    private Long bookingId;

    @NotNull(message = "Payment gateway type must not be null")
    private PaymentGatewayType paymentGatewayType;

    private String voucherCode;
}
