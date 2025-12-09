package com.trainning.movie_booking_system.dto.request.Otp;

import com.trainning.movie_booking_system.utils.enums.OtpType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SendOtpRequest {

    @NotBlank(message = "Email not null")
    @Email(message = "Email not format")
    private String email;

    private OtpType type;
}
