package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Screen.ScreenRequest;
import com.trainning.movie_booking_system.dto.request.Screen.UpdateScreenRequest;
import com.trainning.movie_booking_system.dto.response.Screen.ScreenResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.utils.enums.ScreenStatus;

import java.util.List;

public interface ScreenService {

    /**
     * Create a new screen
     *
     * @param request screen request object
     * @return screen response object
     */
    ScreenResponse create(ScreenRequest request);

    /**
     * Update an existing screen
     *
     * @param screenId screen id
     * @param request  screen request object
     * @return screen response object
     */
    ScreenResponse update(Long screenId, UpdateScreenRequest request);

    /**
     * Delete a screen
     *
     * @param screenId screen id
     * @param status screen status
     */
    void delete(Long screenId, ScreenStatus status);

    /**
     * Get a screen by id
     *
     * @param screenId screen id
     * @return screen response object
     */
    ScreenResponse getById(Long screenId);

    /**
     * Get screens by theater id
     *
     * @param theaterId theater id
     * @return screen response object
     */
    List<ScreenResponse> getByTheater(Long theaterId);

    /**
     * Get all screens with pagination
     *
     * @param pageNumber page number
     * @param pageSize   page size
     * @return paginated screen response
     */
    PageResponse<?> getAll(int pageNumber, int pageSize);

    /**
     * Count all screens
     *
     * @return total number of screens
     */
    long countAllScreens();
}
