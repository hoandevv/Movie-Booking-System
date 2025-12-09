package com.trainning.movie_booking_system.dto.response.Seat;

import com.trainning.movie_booking_system.utils.enums.SeatType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatBookingResponse {

    private Long id;
    private int seatNumber;
    private String rowLabel;
    private SeatType seatType;

    public String getSeatLabel() {
        return rowLabel + seatNumber;
    }
}
