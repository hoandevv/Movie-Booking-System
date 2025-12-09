package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Showtime.ShowtimeRequest;
import com.trainning.movie_booking_system.dto.request.Showtime.UpdateShowtimeRequest;
import com.trainning.movie_booking_system.dto.response.Showtime.ShowtimeByScreenResponse;
import com.trainning.movie_booking_system.dto.response.Showtime.ShowtimeResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.utils.enums.ShowtimeStatus;

import java.time.LocalDate;
import java.util.List;

public interface ShowtimeService {

    /**
     * Create a new showtime.
     *
     * @param request the showtime request data
     * @return the created showtime response
     */
    ShowtimeResponse create(ShowtimeRequest request);

    /**
     * Update an existing showtime.
     *
     * @param id      the ID of the showtime to update
     * @param request the updated showtime request data
     * @return the updated showtime response
     */
    ShowtimeResponse update(Long id, UpdateShowtimeRequest request);

    /**
     * Delete a showtime by its ID.
     *
     * @param id the ID of the showtime to delete
     */
    void delete(Long id, ShowtimeStatus status);

    /**
     * Get a showtime by its ID.
     *
     * @param id the ID of the showtime to retrieve
     * @return the showtime response
     */
    ShowtimeResponse getById(Long id);

    /**
     * Get all showtimes with pagination.
     *
     * @param pageNumber the page number to retrieve
     * @param pageSize   the number of showtimes per page
     * @return a paginated response of showtimes
     */
    PageResponse<?> getAll(int pageNumber, int pageSize);

    /**
     * Get showtimes by theater and movie on a specific date.
     *
     * @param theaterId the ID of the theater
     * @param movieId   the ID of the movie
     * @param date      the date for which to retrieve showtimes
     * @return a list of showtimes grouped by screen
     */
    List<ShowtimeByScreenResponse> findByTheaterAndMovie(Long theaterId,
                                                         Long movieId,
                                                         LocalDate date);

    /**
     * Count the total number of showtimes.
     *
     * @return the total count of showtimes
     */
    long countShowtime();
}
