package com.trainning.movie_booking_system.dto.request.Booking;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BookingRequest {
    @NotNull(message = "Showtime ID must not be null")
    private Long showtimeId;

    private String voucherId;

    @NotNull(message = "Seat IDs must not be null")
    private List<Long> seatIds;
}
