package com.trainning.movie_booking_system.service.impl.Movie;

import com.trainning.movie_booking_system.dto.request.Movie.MovieRequest;
import com.trainning.movie_booking_system.dto.request.Movie.UpdateMovieRequest;
import com.trainning.movie_booking_system.dto.response.Movie.MovieResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.entity.Movie;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.mapper.MovieMapper;
import com.trainning.movie_booking_system.repository.MovieRepository;
import com.trainning.movie_booking_system.service.Movie.MovieService;
import com.trainning.movie_booking_system.utils.enums.MovieStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import static com.trainning.movie_booking_system.mapper.MovieMapper.toMovieResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;

    /**
     * Create a new movie
     *
     * @param request the movie request
     * @return the created movie response
     */
    @Transactional
    @Override
    public MovieResponse create(MovieRequest request) {
        log.info("[MOVIE SERVICE] - Create movie request: {}", request);

        if (movieRepository.existsByTitle(request.getTitle().trim())) {
            log.error("[MOVIE SERVICE] - Movie with title '{}' already exists", request.getTitle());
            throw new BadRequestException("Movie with the given title already exists");
        }

        LocalDate releaseDate = null;
        if (request.getReleaseDate() != null && !request.getReleaseDate().isBlank()) {
            try {
                releaseDate = LocalDate.parse(request.getReleaseDate().trim());
            } catch (DateTimeParseException e) {
                log.error("[MOVIE SERVICE] - Invalid release date: {}", request.getReleaseDate());
                throw new BadRequestException("Invalid release date format. Expected yyyy-MM-dd");
            }
        }

        Movie movie = buildMovie(request, releaseDate);
        movieRepository.save(movie);
        log.info("[MOVIE SERVICE] - Movie created successfully with ID: {}", movie.getId());

        return toMovieResponse(movie);
    }

    /**
     * Update an existing movie
     *
     * @param movieId the ID of the movie to update
     * @param request the movie request
     * @return the updated movie response
     */
    @Transactional
    @Override
    public MovieResponse update(Long movieId, UpdateMovieRequest request) {
        log.info("[MOVIE SERVICE] - Update movie ID: {}, request: {}", movieId, request);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with ID: " + movieId));

        // Chỉ update khi có giá trị mới
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            if (!movie.getTitle().equalsIgnoreCase(request.getTitle().trim())
                    && movieRepository.existsByTitle(request.getTitle().trim())) {
                throw new BadRequestException("Movie with the given title already exists");
            }
            movie.setTitle(request.getTitle().trim());
        }
        updateFiled(request, movie);
        movieRepository.save(movie);
        log.info("[MOVIE SERVICE] - Movie updated successfully: {}", movieId);

        return toMovieResponse(movie);
    }

    /**
     * Delete a movie by its ID
     *
     * @param movieId     the ID of the movie to delete
     * @param movieStatus the status of the movie
     */
    @Transactional
    @Override
    public void delete(Long movieId, MovieStatus movieStatus) {
        log.info("MOVIE SERVICE] - Delete movie with ID: {}", movieId);
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with ID: " + movieId));
        movie.setStatus(MovieStatus.ENDED);
        movieRepository.save(movie);
    }

    /**
     * Get a movie by its ID
     *
     * @param movieId the ID of the movie to retrieve
     * @return the movie response
     */
    @Override
    public MovieResponse getById(Long movieId) {
        log.info("[MOVIE SERVICE] - Get movie with ID: {}", movieId);
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with ID: " + movieId));

        return toMovieResponse(movie);
    }

    /**
     * Get all movies with pagination
     *
     * @param pageNumber the page number
     * @param pageSize the size of the page
     * @return a paginated response of movies
     */
    @Override
    public PageResponse<?> getAll(int pageNumber, int pageSize) {
        log.info("[MOVIE SERVICE] - Get all movies - Page: {}, Size: {}", pageNumber, pageSize);

        if (pageNumber < 0 || pageSize < 0) {
            throw new BadRequestException("Invalid PageNumber and PageSize");
        }

        Page<MovieResponse> movieResponse = movieRepository.findAll(PageRequest.of(pageNumber, pageSize))
                .map(MovieMapper::toMovieResponse);
        return PageResponse.of(movieResponse);
    }

    /**
     * Count total number of movies
     *
     * @return the total count of movies
     */
    @Override
    public long countTotalMovies() {
        log.info("[MOVIE SERVICE] - Counting total movies");
        return movieRepository.count();
    }

    //====================================== Private methods =====================================//
    private static Movie buildMovie(MovieRequest request, LocalDate releaseDate) {
        return Movie.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .genre(request.getGenre())
                .language(request.getLanguage())
                .duration(request.getDuration())
                .releaseDate(releaseDate)
                .posterUrl(request.getPosterUrl())
                .trailerUrl(request.getTrailerUrl())
                .rating(request.getRating() != null ? BigDecimal.valueOf(request.getRating()) : null)
                .status(request.getStatus())
                .build();
    }

    private static void updateFiled(UpdateMovieRequest request, Movie movie) {
        if (request.getDescription() != null)
            movie.setDescription(request.getDescription());

        if (request.getDuration() != null)
            movie.setDuration(request.getDuration());

        if (request.getReleaseDate() != null && !request.getReleaseDate().isBlank()) {
            try {
                movie.setReleaseDate(LocalDate.parse(request.getReleaseDate().trim()));
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Invalid release date format. Expected yyyy-MM-dd");
            }
        }

        if (request.getPosterUrl() != null)
            movie.setPosterUrl(request.getPosterUrl());

        if (request.getTrailerUrl() != null)
            movie.setTrailerUrl(request.getTrailerUrl());

        if (request.getRating() != null)
            movie.setRating(BigDecimal.valueOf(request.getRating()));

        if (request.getGenre() != null)
            movie.setGenre(request.getGenre());

        if (request.getLanguage() != null)
            movie.setLanguage(request.getLanguage());

        if (request.getStatus() != null)
            movie.setStatus(request.getStatus());
    }

}
