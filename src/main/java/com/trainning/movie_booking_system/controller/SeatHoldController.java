package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Seat.HoldSeatsRequest;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.entity.Seat;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.helper.redis.SeatDomainService;
import com.trainning.movie_booking_system.repository.SeatRepository;
import com.trainning.movie_booking_system.repository.ShowtimeRepository;
import com.trainning.movie_booking_system.security.SecurityUtils;
import com.trainning.movie_booking_system.service.SeatHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

/**
 * Seat Hold Controller - Xử lý hold/release seats tạm thời
 * User hold seats trước khi create booking
 * 
 * REQUIRES AUTHENTICATION - User must be logged in
 */
@RestController
@RequestMapping("/api/v1/seat-holds")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SeatHoldController {

    private final SeatHoldService seatHoldService;
    /**
     * Hold seats tạm thời (default 120s)
     * User phải gọi endpoint này TRƯỚC KHI create booking
     * 
     * REQUIRES AUTHENTICATION - User must be logged in
     * POST /api/v1/seat-holds
     * 
     * @param req Hold request with showtimeId, seatIds, ttlSec
     * @return Success message
     */
    @PostMapping
    public ResponseEntity<?> hold(@RequestBody @Valid HoldSeatsRequest req) {
        var userId = SecurityUtils.getCurrentUserDetails().getAccount().getId();
        return ResponseEntity.ok(BaseResponse.success(seatHoldService.holdSeats(req, userId)));
    }


    /**
     * Release held seats manually
     * User có thể gọi nếu muốn hủy hold trước khi hết TTL
     * 
     * REQUIRES AUTHENTICATION - User must be logged in
     * DELETE /api/v1/seat-holds
     * 
     * @param req Release request with showtimeId, seatIds
     * @return Success message
     */
    @DeleteMapping
    public ResponseEntity<?> release(@RequestBody @Valid HoldSeatsRequest req) {
        seatHoldService.releaseSeats(req);
        return ResponseEntity.ok(BaseResponse.success(null, "Seats released successfully"));
    }
}