package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.response.Theater.TheaterResponse;
import com.trainning.movie_booking_system.entity.Theater;

public class TheaterMapper {

    /**
     * Map Theater entity to TheaterResponse DTO
     * @param theater Theater entity
     * @return TheaterResponse DTO
     */
    public static TheaterResponse toTheaterResponse(Theater theater) {
        return TheaterResponse.builder()
                .id(theater.getId())
                .name(theater.getName())
                .location(theater.getLocation())
                .city(theater.getCity())
                .phone(theater.getPhone())
                .status(theater.getStatus())
                .build();
    }
}
