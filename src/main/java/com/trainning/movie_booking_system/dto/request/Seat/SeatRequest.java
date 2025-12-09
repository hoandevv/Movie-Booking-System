package com.trainning.movie_booking_system.dto.request.Seat;

import com.trainning.movie_booking_system.utils.enums.SeatStatus;
import com.trainning.movie_booking_system.utils.enums.SeatType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatRequest {
    @NotNull(message = "Screen ID is required")
    private Long screenId;

    @NotBlank(message = "Row label is required")
    @Size(max = 10, message = "Row label must not exceed 10 characters")
    private String rowLabel;

    @NotNull(message = "Seat number is required")
    @Min(value = 1, message = "Seat number must be greater than 0")
    private Integer seatNumber;

    @NotNull(message = "Seat type is required")
    private SeatType seatType;

    @NotNull(message = "Status is required")
    private SeatStatus status;
}
