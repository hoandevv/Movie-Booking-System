package com.trainning.movie_booking_system.dto.request.Auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LogoutRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    public LogoutRequest() {}
    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

}