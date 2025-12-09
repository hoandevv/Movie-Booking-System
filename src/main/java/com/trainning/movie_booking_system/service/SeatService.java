package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Seat.SeatRequest;
import com.trainning.movie_booking_system.dto.request.Seat.SeatGenerationRequest;
import com.trainning.movie_booking_system.dto.response.Seat.SeatResponse;
import com.trainning.movie_booking_system.utils.enums.SeatStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SeatService {

    /**
     * Create a new seat
     *
     * @param request seat request object
     * @return seat response object
     */
    SeatResponse create(SeatRequest request);

    /**
     * Update an existing seat
     *
     * @param seatId  seat id
     * @param request seat request object
     * @return seat response object
     */
    SeatResponse update(Long seatId, SeatRequest request);

    /**
     * Delete a seat
     *
     * @param seatId seat id
     */
    void delete(Long seatId);

    /**
     * Get seat by id
     *
     * @param seatId seat id
     * @return seat response object
     */
    SeatResponse getById(Long seatId);

    /**
     * Get all seats with pagination
     *
     * @param pageNumber page number
     * @param pageSize   page size
     * @return paginated list of seat response objects
     */
    Page<SeatResponse> getAlls(int pageNumber, int pageSize);

    /**
     * Get all seats by screen ID
     *
     * @param screenId screen id
     * @return list of seat response objects
     */
    List<SeatResponse> getByScreenId(Long screenId);
    
    /**
     * Get seats by screen ID (alias for getByScreenId)
     *
     * @param screenId screen id
     * @return list of seat response objects
     */
    default List<SeatResponse> getSeatsByScreenId(Long screenId) {
        return getByScreenId(screenId);
    }
    
    /**
     * Generate seats for a screen based on the provided configuration
     * 
     * @param screenId the ID of the screen
     * @param request the seat generation configuration
     * @return list of created seat responses
     */
    List<SeatResponse> generateSeats(Long screenId, SeatGenerationRequest request);

    /**
     * Get seats by screen ID and status
     *
     * @param screenId screen id
     * @param status   seat status
     * @return list of seat response objects
     */
    List<SeatResponse> getSeatsByScreenIdAndStatus(Long screenId, SeatStatus status);
}
