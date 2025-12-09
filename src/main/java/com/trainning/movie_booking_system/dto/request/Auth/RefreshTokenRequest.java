package com.trainning.movie_booking_system.dto.request.Auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token not nulll")
    private String refreshToken;
}
