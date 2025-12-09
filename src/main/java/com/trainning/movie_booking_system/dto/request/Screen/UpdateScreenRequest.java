package com.trainning.movie_booking_system.dto.request.Screen;

import com.trainning.movie_booking_system.utils.enums.ScreenStatus;
import jakarta.validation.constraints.Min;
import lombok.Getter;

@Getter
public class UpdateScreenRequest {
    private String name;

    @Min(value = 1, message = "Total seats must be at least 1")
    private Integer totalSeats;
    private ScreenStatus status;
    private Long theaterId;
}
