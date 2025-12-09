package com.trainning.movie_booking_system.dto.request.Screen;

import com.trainning.movie_booking_system.utils.enums.ScreenStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ScreenRequest {

    @NotBlank(message = "Screen name must not be blank")
    private String name;

    @Min(value = 1, message = "Total seats must be at least 1")
    @NotNull(message = "Total seats must not be null")
    private Integer totalSeats;

    @NotNull(message = "Screen status must not be blank")
    private ScreenStatus status;

    @NotNull(message = "Theater ID must not be null")
    private Long theaterId;
}
