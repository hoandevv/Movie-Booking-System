package com.trainning.movie_booking_system.dto.response.Booking;

import com.trainning.movie_booking_system.dto.response.Seat.SeatBookingResponse;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class BookingSeatResponse {
    private Long id;
    private SeatBookingResponse seat;
    private BigDecimal price;
}
