package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Movie.MovieRequest;
import com.trainning.movie_booking_system.dto.request.Movie.MovieSearchFilter;
import com.trainning.movie_booking_system.dto.request.Movie.UpdateMovieRequest;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.service.Movie.MovieSearchService;
import com.trainning.movie_booking_system.service.Movie.MovieService;
import com.trainning.movie_booking_system.utils.enums.MovieStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
@Validated
@Slf4j
public class MovieController {

    private final MovieService movieService;
    private final MovieSearchService movieSearchService;

    /**
     * Create a new movie
     *
     * @param request the movie request
     * @return the created movie response
     */
    @PreAuthorize(value = "hasAnyAuthority('ROLE_ADMIN', 'ROLE_THEATER_MANAGEMENT')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid MovieRequest request) {
        log.info("[MOVIE-CONTROLLER] Create movie request: {}", request);
        return ResponseEntity.ok(BaseResponse.success(movieService.create(request)));
    }

    /**
     * Update an existing movie
     * @param movieId the movie ID
     * @param request the movie request
     * @return the updated movie response
     */
    @PreAuthorize(value = "hasAnyAuthority('ROLE_ADMIN', 'ROLE_THEATER_MANAGEMENT')")
    @PatchMapping("/{movieId}")
    public ResponseEntity<?> update(@PathVariable Long movieId, @RequestBody @Valid UpdateMovieRequest request) {
        log.info("[MOVIE-CONTROLLER] Update movie request: {}, {}", movieId, request);
        return ResponseEntity.ok(BaseResponse.success(movieService.update(movieId, request)));
    }

    /**
     * Delete a movie by its ID
     *
     * @param movieId     the ID of the movie to delete
     * @return ResponseEntity indicating the result of the delete operation
     */
    @PreAuthorize(value = "hasAnyAuthority('ROLE_ADMIN', 'ROLE_THEATER_MANAGEMENT')")
    @DeleteMapping("/{movieId}")
    public ResponseEntity<?> delete(@PathVariable Long movieId, @RequestParam MovieStatus movieStatus) {
        log.info("[MOVIE-CONTROLLER] Delete movie request: {}, {}", movieId, movieStatus);
        movieService.delete(movieId, movieStatus);
        return ResponseEntity.ok(BaseResponse.success());
    }

    /**
     * Get a movie by its ID
     * PUBLIC - No authentication required
     * @param movieId the ID of the movie to retrieve
     * @return the movie response
     */
    @GetMapping("/{movieId}")
    public ResponseEntity<?> getById(@PathVariable Long movieId) {
        log.info("[MOVIE-CONTROLLER] Get movie by ID request: {}", movieId);
        return ResponseEntity.ok(BaseResponse.success(movieService.getById(movieId)));
    }

    /**
     * Get all movies with pagination
     * PUBLIC - No authentication required
     * @param pageNumber the page number
     * @param pageSize the size of the page
     * @return a paginated response of movies
     */
    @GetMapping
    public ResponseEntity<?> getAlls(@RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
                                     @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        log.info("[MOVIE-CONTROLLER] Get all movies request: {}, {}", pageSize, pageNumber);
        return ResponseEntity.ok(BaseResponse.success(movieService.getAll(pageNumber, pageSize)));
    }


    /**
     * Search movies based on filters with pagination
     * PUBLIC - No authentication required
     *
     * @param keyword      keyword to search in title or description
     * @param genres       set of genres to filter
     * @param language     language to filter
     * @param status       movie status to filter
     * @param ratingMin    minimum rating to filter
     * @param ratingMax    maximum rating to filter
     * @param durationMin  minimum duration to filter
     * @param durationMax  maximum duration to filter
     * @param releaseFrom  release date from to filter
     * @param releaseTo    release date to filter
     * @param page         page number for pagination
     * @param size         page size for pagination
     * @param sortBy       field to sort by
     * @param direction    sort direction (ASC or DESC)
     * @return a paginated response of movies matching the search criteria
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchMovies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Set<String> genres,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal ratingMin,
            @RequestParam(required = false) BigDecimal ratingMax,
            @RequestParam(required = false) Integer durationMin,
            @RequestParam(required = false) Integer durationMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseTo,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(defaultValue = "releaseDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        MovieSearchFilter filter = new MovieSearchFilter();
        filter.setKeyword(keyword);
        filter.setGenres(genres);
        filter.setLanguage(language);
        if (status != null) {
            try {
                filter.setStatus(MovieStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid movie status: " + status);
            }
        }
        filter.setRatingMin(ratingMin);
        filter.setRatingMax(ratingMax);
        filter.setDurationMin(durationMin);
        filter.setDurationMax(durationMax);
        filter.setReleaseFrom(releaseFrom);
        filter.setReleaseTo(releaseTo);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setDirection(direction);
        return ResponseEntity.ok(BaseResponse.success(movieSearchService.search(filter)));
    }

    /**
     * Count total number of movies
     * @return the total count of movies
     */
    @GetMapping("/count")
    public ResponseEntity<?> countTotalMovies() {
        log.info("[MOVIE-CONTROLLER] Count total movies request");
        return ResponseEntity.ok(BaseResponse.success(movieService.countTotalMovies()));
    }
}
