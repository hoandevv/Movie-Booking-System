package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Seat.HoldSeatsRequest;
import com.trainning.movie_booking_system.dto.response.Seat.HoldSeatsResponse;

public interface SeatHoldService {

    HoldSeatsResponse holdSeats(HoldSeatsRequest request, Long userId);

    void releaseSeats(HoldSeatsRequest request);
}
