package com.trainning.movie_booking_system.dto.request.Theater;

import com.trainning.movie_booking_system.utils.enums.TheaterStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTheaterRequest {
    private String name;
    private String location;
    private String city;
    private String phone;
    private TheaterStatus status;
}
