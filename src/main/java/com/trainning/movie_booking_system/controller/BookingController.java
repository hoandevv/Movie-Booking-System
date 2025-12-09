package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Booking.BookingRequest;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.service.BookingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Validated
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

    /**
     * Create a new booking
     * Sau khi tạo thành công, user cần redirect sang payment gateway
     * 
     * REQUIRES AUTHENTICATION - User must be logged in
     *
     * @param request the booking request data
     * @return response entity with created booking
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid BookingRequest request) {
        log.info("[BOOKING] Create request: {}", request);
        var booking = bookingService.create(request);

        // TODO: Frontend cần redirect user sang /api/v1/bookings/{bookingId}/payment
        log.info("[BOOKING] Booking {} created. Status: PENDING_PAYMENT. User needs to complete payment within 15 minutes.",
                booking.getId());

        return ResponseEntity.ok(BaseResponse.success(booking,
                "Booking created successfully. Please complete payment within 15 minutes."));
    }

    /**
     * Update an existing booking
     *
     * @param bookingId the ID of the booking to update
     * @param request   the updated booking request data
     * @return response entity with updated booking
     */
    @PatchMapping("/{bookingId}")
    public ResponseEntity<?> update(@PathVariable Long bookingId, @RequestBody @Valid BookingRequest request) {
        log.info("[BOOKING] Update request: id={}, body={}", bookingId, request);
        return ResponseEntity.ok(BaseResponse.success(bookingService.update(bookingId, request)));
    }

    /**
     * Delete a booking by ID
     *
     * @param bookingId the ID of the booking to delete
     * @return response entity with deletion result
     */
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<?> delete(@PathVariable Long bookingId) {
        log.info("[BOOKING] Delete request: id={}", bookingId);
        bookingService.delete(bookingId);
        return ResponseEntity.ok(BaseResponse.success());
    }

    /**
     * Get a booking by ID
     *
     * @param bookingId the ID of the booking to retrieve
     * @return response entity with booking data
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getById(@PathVariable Long bookingId) {
        log.info("[BOOKING] Get by id: {}", bookingId);
        return ResponseEntity.ok(BaseResponse.success(bookingService.getById(bookingId)));
    }

    /**
     * Get all bookings with pagination
     *
     * @param pageNumber the page number to retrieve
     * @param pageSize   the number of bookings per page
     * @return response entity with paginated bookings
     */
    @GetMapping
    public ResponseEntity<?> getAlls(@RequestParam int pageNumber, @RequestParam int pageSize) {
        log.info("[BOOKING] Get all bookings: page={}, size={}", pageNumber, pageSize);
        return ResponseEntity.ok(BaseResponse.success(bookingService.getAlls(pageNumber, pageSize)));
    }
}
