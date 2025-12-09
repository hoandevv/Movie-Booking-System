package com.trainning.movie_booking_system.dto.request.Showtime;

import com.trainning.movie_booking_system.utils.enums.ShowtimeStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateShowtimeRequest implements Serializable {

    private Long movieId;

    private Long screenId;

    private LocalDate showDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    private ShowtimeStatus status;
}
