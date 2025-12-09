package com.trainning.movie_booking_system.dto.response.Auth;

import lombok.*;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
}


