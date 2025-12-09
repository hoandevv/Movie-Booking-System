package com.trainning.movie_booking_system.service.Movie;

import com.trainning.movie_booking_system.dto.request.Movie.MovieSearchFilter;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;

public interface MovieSearchService {
    /**
     * Search movies based on filters with pagination
     *
     * @param filter the movie search filter
     * @return a paginated response of movies matching the search criteria
     */
    PageResponse<?> search(MovieSearchFilter filter);
}
