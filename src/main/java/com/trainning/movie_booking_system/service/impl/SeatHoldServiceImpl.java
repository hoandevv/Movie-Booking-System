package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Seat.HoldSeatsRequest;
import com.trainning.movie_booking_system.dto.response.Seat.HoldSeatsResponse;
import com.trainning.movie_booking_system.entity.Seat;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.helper.redis.SeatDomainService;
import com.trainning.movie_booking_system.repository.SeatRepository;
import com.trainning.movie_booking_system.repository.ShowtimeRepository;
import com.trainning.movie_booking_system.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatHoldServiceImpl implements SeatHoldService {

    private final SeatDomainService seatDomainService;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;

    @Override
    public HoldSeatsResponse holdSeats(HoldSeatsRequest request, Long userId) {
        // Validate showtime exists
        showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new NotFoundException("Showtime not found with ID: " + request.getShowtimeId()));

        // Validate seats exist
        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
        if (seats.size() != request.getSeatIds().size()) {
            List<Long> foundIds = seats.stream().map(Seat::getId).toList();
            List<Long> missingIds = request.getSeatIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new BadRequestException("Seats not found: " + missingIds);
        }

        int ttlSec = request.getTtlSec() != null ? request.getTtlSec() : 120;
        seatDomainService.holdSeats(request.getShowtimeId(), request.getSeatIds(), userId, Duration.ofSeconds(ttlSec));

        return HoldSeatsResponse.builder()
                .showtimeId(request.getShowtimeId())
                .heldSeatIds(request.getSeatIds())
                .ttlSec(ttlSec)
                .message("Seats held successfully for %d seconds. Please create booking before timeout.".formatted(ttlSec))
                .build();
    }

    @Override
    public void releaseSeats(HoldSeatsRequest request) {
        seatDomainService.releaseHolds(request.getShowtimeId(), request.getSeatIds());
    }
}
