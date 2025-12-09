package com.trainning.movie_booking_system.service.Movie;

import com.trainning.movie_booking_system.dto.request.Movie.MovieRequest;
import com.trainning.movie_booking_system.dto.request.Movie.UpdateMovieRequest;
import com.trainning.movie_booking_system.dto.response.Movie.MovieResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.utils.enums.MovieStatus;

public interface MovieService {

    /**
     * Create a new movie
     * @param request the movie request
     * @return the created movie response
     */
    MovieResponse create(MovieRequest request);

    /**
     * Update an existing movie
     *
     * @param movieId the ID of the movie to update
     * @param request the movie request
     * @return the updated movie response
     */
    MovieResponse update(Long movieId, UpdateMovieRequest request);

    /**
     * Delete a movie by its ID
     *
     * @param movieId     the ID of the movie to delete
     * @param movieStatus the status of the movie
     */
    void delete(Long movieId, MovieStatus movieStatus);

    /**
     * Get a movie by its ID
     * @param movieId the ID of the movie to retrieve
     * @return the movie response
     */
    MovieResponse getById(Long movieId);

    /**
     * Get all movies with pagination
     * @param pageSize the page number
     * @param pageNumber the size of the page
     * @return a paginated response of movies
     */
    PageResponse<?> getAll(int pageSize, int pageNumber);

    /**
     * Count total number of movies
     * @return the total count of movies
     */
    long countTotalMovies();
}
