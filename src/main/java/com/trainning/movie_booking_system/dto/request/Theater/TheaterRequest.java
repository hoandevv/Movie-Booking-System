package com.trainning.movie_booking_system.dto.request.Theater;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class TheaterRequest {
    @NotBlank(message = "Theater name is required")
    private String name;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Phone number is required")
    private String phone;
}
