package com.trainning.movie_booking_system.dto.response.Theater;

import com.trainning.movie_booking_system.utils.enums.TheaterStatus;
import lombok.Builder;
import lombok.Getter;
import java.io.Serializable;

@Getter
@Builder
public class TheaterResponse implements Serializable {
    private Long id;
    private String name;
    private String location;
    private String city;
    private String phone;
    private TheaterStatus status;
}
