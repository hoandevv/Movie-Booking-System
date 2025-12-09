package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Booking.BookingRequest;
import com.trainning.movie_booking_system.dto.response.Booking.BookingResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;

public interface BookingService {

    /**
     * Create a new booking
     *
     * @param request the booking request data
     * @return the created booking response
     */
    BookingResponse create(BookingRequest request);

    /**
     * Update an existing booking
     *
     * @param id      the ID of the booking to update
     * @param request the updated booking request data
     * @return the updated booking response
     */
    BookingResponse update(Long id, BookingRequest request);

    /**
     * Delete a booking by ID
     *
     * @param id the ID of the booking to delete
     */
    void delete(Long id);

    /**
     * Get a booking by ID
     *
     * @param id the ID of the booking to retrieve
     * @return the booking response
     */
    BookingResponse getById(Long id);

    /**
     * Get all bookings with pagination
     *
     * @param pageNumber the page number to retrieve
     * @param pageSize   the number of bookings per page
     * @return a paginated response of bookings
     */
    PageResponse<?> getAlls(int pageNumber, int pageSize);
}
