package com.trainning.movie_booking_system.dto.request.Admin;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;

    @Pattern(
            regexp = "^\\d{10,11}$",
            message = "Phone number must be 10–11 digits"
    )
    private String phoneNumber;

    // true = staff, false = user
    private Boolean isStaff;

    // true = active, false = inactive/locked
    private Boolean isActive;
}
