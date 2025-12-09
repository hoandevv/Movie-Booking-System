package com.trainning.movie_booking_system.dto.response.Screen;

import com.trainning.movie_booking_system.dto.response.Theater.TheaterResponse;
import lombok.Builder;
import lombok.Getter;
import java.io.Serializable;

@Getter
@Builder
public class ScreenResponse implements Serializable {
    private Long id;
    private String name;
    private Integer totalSeats;
    private String status;
    private TheaterResponse theaterResponse;
}
