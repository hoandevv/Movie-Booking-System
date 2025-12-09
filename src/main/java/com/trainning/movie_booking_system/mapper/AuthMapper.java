package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.response.Auth.AuthResponse;

public class AuthMapper {

    public static AuthResponse toResponse(String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
