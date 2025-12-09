package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Seat.SeatRequest;
import com.trainning.movie_booking_system.dto.request.Seat.SeatGenerationRequest;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.service.SeatService;
import com.trainning.movie_booking_system.utils.enums.SeatStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SeatController {

    private final SeatService seatService;

    /**
     * Create a new seat
     *
     * @param request seat request object
     * @return seat response object
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> create(@RequestBody @Valid SeatRequest request) {
        log.info("[SEAT-CONTROLLER] Create seat request: {}", request);
        return ResponseEntity.ok(BaseResponse.success(seatService.create(request)));
    }

    /**
     * Update an existing seat
     *
     * @param seatId  seat id
     * @param request seat request object
     * @return seat response object
     */
    @PatchMapping("/{seatId}")
    public ResponseEntity<?> update(
            @PathVariable Long seatId,
            @RequestBody @Valid SeatRequest request
    ) {
        log.info("[SEAT-CONTROLLER] Update seat request: {}, {}", seatId, request);
        return ResponseEntity.ok(BaseResponse.success(seatService.update(seatId, request)));
    }

    /**
     * Delete a seat
     *
     * @param seatId seat id
     */
    @DeleteMapping("/{seatId}")
    public ResponseEntity<?> delete(@PathVariable Long seatId) {
        log.info("[SEAT-CONTROLLER] Delete seat request: {}", seatId);
        seatService.delete(seatId);
        return ResponseEntity.ok(BaseResponse.success("Seat deleted successfully"));
    }

    /**
     * Generate seats for a screen
     *
     * @param screenId the ID of the screen
     * @param request the seat generation configuration
     * @return list of created seats
     */
    @PostMapping("/generate/{screenId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> generateSeats(
            @PathVariable Long screenId,
            @RequestBody @Valid SeatGenerationRequest request) {
        log.info("[SEAT-CONTROLLER] Generate seats request for screen {}: {}", screenId, request);
        return ResponseEntity.ok(BaseResponse.success(seatService.generateSeats(screenId, request)));
    }

    /**
     * Get seat by id
     *
     * @param seatId seat id
     * @return seat response object
     */
    @GetMapping("/{seatId}")
    public ResponseEntity<?> getById(@PathVariable Long seatId) {
        log.info("[SEAT-CONTROLLER] Get seat by id request: {}", seatId);
        return ResponseEntity.ok(BaseResponse.success(seatService.getById(seatId)));
    }

    /**
     * Get all seats with pagination
     *
     * @param pageNumber page number
     * @param pageSize   page size
     * @return paginated list of seat response objects
     */
    @GetMapping
    public ResponseEntity<?> getAlls(
            @RequestParam int pageNumber,
            @RequestParam int pageSize
    ) {
        log.info("[SEAT-CONTROLLER] Get all seats request: {}, {}", pageNumber, pageSize);
        return ResponseEntity.ok(BaseResponse.success(seatService.getAlls(pageNumber, pageSize)));
    }

    /**
     * Get all seats by screen ID
     *
     * @param screenId screen id
     * @return list of seat response objects
     */
    @GetMapping("/screen/{screenId}")
    public ResponseEntity<?> getSeatsByScreenId(@PathVariable Long screenId) {
        log.info("[SEAT-CONTROLLER] Get seats by screen id request: {}", screenId);
        return ResponseEntity.ok(BaseResponse.success(seatService.getSeatsByScreenId(screenId)));
    }

    /**
     * Get seats by screen ID and status
     *
     * @param screenId screen id
     * @param status   seat status
     * @return list of seat response objects
     */
    @GetMapping("/screen/{screenId}/status/{status}")
    public ResponseEntity<?> getSeatsByScreenIdAndStatus(
            @PathVariable Long screenId,
            @PathVariable SeatStatus status
    ) {
        log.info("[SEAT-CONTROLLER] Get seats by screen id: {} and status: {}", screenId, status);
        return ResponseEntity.ok(BaseResponse.success(
                seatService.getSeatsByScreenIdAndStatus(screenId, status)
        ));
    }
}
