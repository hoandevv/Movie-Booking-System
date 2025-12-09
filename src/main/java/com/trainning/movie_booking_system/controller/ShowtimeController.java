package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Showtime.ShowtimeRequest;
import com.trainning.movie_booking_system.dto.request.Showtime.UpdateShowtimeRequest;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.service.ShowtimeService;
import com.trainning.movie_booking_system.utils.enums.ShowtimeStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/showtimes")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    /**
     * Create a new showtime
     *
     * @param request showtime request object
     * @return showtime response object
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid ShowtimeRequest request) {
        log.info("[SHOWTIME-CONTROLLER] Create showtime request: {}", request);
        return ResponseEntity.ok(BaseResponse.success(showtimeService.create(request)));
    }

    /**
     * Update an existing showtime
     *
     * @param showtimeId showtime id
     * @param request    showtime request object
     * @return showtime response object
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @PatchMapping("/{showtimeId}")
    public ResponseEntity<?> update(@PathVariable Long showtimeId, @RequestBody @Valid UpdateShowtimeRequest request) {
        log.info("[SHOWTIME-CONTROLLER] Update showtime request: {}, {}", showtimeId, request);
        return ResponseEntity.ok(BaseResponse.success(showtimeService.update(showtimeId, request)));
    }

    /**
     * Delete a showtime (soft delete)
     *
     * @param showtimeId showtime id
     * @param status     showtime status
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{showtimeId}")
    public ResponseEntity<?> delete(@PathVariable Long showtimeId, @RequestParam ShowtimeStatus status) {
        log.info("[SHOWTIME-CONTROLLER] Delete showtime request: {}, with status: {}", showtimeId, status);
        showtimeService.delete(showtimeId, status);
        return ResponseEntity.ok(BaseResponse.success());
    }

    /**
     * Get a showtime by id
     * PUBLIC - No authentication required
     *
     * @param showtimeId showtime id
     * @return showtime response object
     */
    @GetMapping("/{showtimeId}")
    public ResponseEntity<?> getById(@PathVariable Long showtimeId) {
        log.info("[SHOWTIME-CONTROLLER] Get showtime by id request: {}", showtimeId);
        return ResponseEntity.ok(BaseResponse.success(showtimeService.getById(showtimeId)));
    }

    /**
     * Get all showtimes with pagination and optional filters
     * PUBLIC - No authentication required
     * 
     * Query params:
     * - theaterId, movieId, date: Filter showtimes
     * - pageNumber, pageSize: Pagination
     *
     * Examples:
     * - GET /api/v1/showtimes?theaterId=1&movieId=2&date=2025-11-11
     * - GET /api/v1/showtimes?pageNumber=0&pageSize=20
     *
     * @param theaterId theater id (optional)
     * @param movieId movie id (optional)
     * @param date date to filter showtimes (optional)
     * @param pageNumber page number
     * @param pageSize page size
     * @return paginated showtime response or filtered list
     */
    @GetMapping
    public ResponseEntity<?> getShowtimes(
            @RequestParam(required = false) Long theaterId,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize) {

        log.info("[SHOWTIME CONTROLLER] Filter: theaterId={}, movieId={}, date={}, page={}, size={}", 
                 theaterId, movieId, date, pageNumber, pageSize);
        
        // If specific filter requested, use filter method
        if (theaterId != null && movieId != null && date != null) {
            return ResponseEntity.ok(BaseResponse.success(
                    showtimeService.findByTheaterAndMovie(theaterId, movieId, date)
            ));
        }
        
        // Otherwise return paginated list
        return ResponseEntity.ok(BaseResponse.success(
                showtimeService.getAll(pageNumber, pageSize)
        ));
    }


    /**
     * Count total number of showtimes
     *
     * @return total count of showtimes
     */
    @GetMapping("/count")
    public ResponseEntity<?> countShowtimes() {
        log.info("[SHOWTIME-CONTROLLER] Count showtimes request");
        return ResponseEntity.ok(BaseResponse.success(showtimeService.countShowtime()));
    }
}
