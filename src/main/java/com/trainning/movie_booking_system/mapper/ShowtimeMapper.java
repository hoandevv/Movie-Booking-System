package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.response.Showtime.ShowtimeResponse;
import com.trainning.movie_booking_system.entity.Showtime;

public class ShowtimeMapper {

    public static ShowtimeResponse toShowtimeResponse(Showtime showtime) {
        return ShowtimeResponse.builder()
                .id(showtime.getId())
                .movie(MovieMapper.toMovieResponse(showtime.getMovie()))
                .screen(ScreenMapper.toScreenResponse(showtime.getScreen()))
                .theater(TheaterMapper.toTheaterResponse(showtime.getScreen().getTheater()))
                .showDate(showtime.getShowDate())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .price(showtime.getPrice())
                .status(showtime.getStatus())
                .build();
    }
}
