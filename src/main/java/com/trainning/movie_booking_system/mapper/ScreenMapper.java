package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.response.Screen.ScreenResponse;
import com.trainning.movie_booking_system.entity.Screen;

public class ScreenMapper {

    public static ScreenResponse toScreenResponse(Screen screen) {
        return ScreenResponse.builder()
                .id(screen.getId())
                .name(screen.getName())
                .totalSeats(screen.getTotalSeats())
                .status(screen.getStatus().name())
                .theaterResponse(TheaterMapper.toTheaterResponse(screen.getTheater()))
                .build();
    }
}
