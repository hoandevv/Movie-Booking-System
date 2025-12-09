package com.trainning.movie_booking_system.dto.request.Seat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * Request DTO for holding seats temporarily
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class HoldSeatsRequest {
    
    @NotNull(message = "Showtime ID is required")
    @Positive(message = "Showtime ID must be positive")
    private Long showtimeId;
    
    @NotEmpty(message = "Seat IDs list must not be empty")
    private List<@NotNull @Positive Long> seatIds;
    
    @Positive(message = "TTL must be positive")
    private Integer ttlSec; // optional, default 120
}