package com.trainning.movie_booking_system.dto.response.Showtime;

import com.trainning.movie_booking_system.dto.response.Movie.MovieResponse;
import com.trainning.movie_booking_system.dto.response.Screen.ScreenResponse;
import com.trainning.movie_booking_system.dto.response.Theater.TheaterResponse;
import com.trainning.movie_booking_system.utils.enums.ShowtimeStatus;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeResponse implements Serializable {

    private Long id;

    private MovieResponse movie;

    private ScreenResponse screen;

    private TheaterResponse theater;

    private LocalDate showDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private BigDecimal price;

    private ShowtimeStatus status;
}
