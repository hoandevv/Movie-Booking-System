package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Theater.TheaterRequest;
import com.trainning.movie_booking_system.dto.request.Theater.UpdateTheaterRequest;
import com.trainning.movie_booking_system.dto.response.Movie.MovieResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.dto.response.Theater.TheaterResponse;
import com.trainning.movie_booking_system.entity.Movie;
import com.trainning.movie_booking_system.entity.Theater;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.mapper.MovieMapper;
import com.trainning.movie_booking_system.mapper.TheaterMapper;
import com.trainning.movie_booking_system.repository.ShowtimeRepository;
import com.trainning.movie_booking_system.repository.TheaterRepository;
import com.trainning.movie_booking_system.service.TheaterService;
import com.trainning.movie_booking_system.utils.enums.TheaterStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import static com.trainning.movie_booking_system.mapper.TheaterMapper.toTheaterResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class TheaterServiceImpl  implements TheaterService {

    private final TheaterRepository theaterRepository;
    private final ShowtimeRepository showtimeRepository;

    /**
     * Create a new theater
     *
     * @param request theater request object
     * @return theater response object
     */
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "theaters:all", allEntries = true),
            @CacheEvict(value = "theaters:detail", allEntries = true)
    })
    public TheaterResponse create(TheaterRequest request) {
        log.info("[THEATER SERVICE] Creating new theater with name: {}", request.getName());

        if (theaterRepository.existsByName(request.getName())) {
            log.error("[THEATER SERVICE] Theater with name '{}' already exists", request.getName());
            throw new BadRequestException("Theater with the same name already exists");
        }

        Theater theater = buildTheater(request);

        theaterRepository.save(theater);

        log.info("[THEATER SERVICE] Theater '{}' created successfully with ID: {}", theater.getName(), theater.getId());

        return toTheaterResponse(theater);
    }

    /**
     * Update an existing theater
     *
     * @param theaterId theater id
     * @param request   theater request object
     * @return theater response object
     */
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "theaters:all", allEntries = true),
            @CacheEvict(value = "theater:detail", key = "#theaterId")
    })
    public TheaterResponse update(Long theaterId, UpdateTheaterRequest request) {
        log.info("[THEATER SERVICE] Updating theater with ID: {}", theaterId);

        Theater theater = getTheaterById(theaterId);

        checkExistsAndUpdateField(request, theater);

        theaterRepository.save(theater);
        log.info("[THEATER SERVICE] Theater with ID '{}' updated successfully", theaterId);

        return toTheaterResponse(theater);
    }

    /**
     * Delete a theater
     *
     * @param theaterId theater id
     * @param status  theater status
     */
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "theaters:all", allEntries = true),
            @CacheEvict(value = "theater:detail", key = "#theaterId")
    })
    public void delete(Long theaterId, TheaterStatus status) {
        log.info("[THEATER SERVICE] Deleting theater with ID: {}", theaterId);
        Theater theater = getTheaterById(theaterId);
        theater.setStatus(status);
        theaterRepository.save(theater);
    }

    /**
     * Get a theater by id
     *
     * @param theaterId theater id
     * @return theater response object
     */
    @Override
    public TheaterResponse getById(Long theaterId) {
        log.info("[THEATER SERVICE] Fetching theater with ID: {}", theaterId);
        return toTheaterResponse(getTheaterById(theaterId));
    }

    /**
     * Get all theaters with pagination
     *
     * @param pageNumber page number
     * @param pageSize   page size
     * @return paginated theater response
     */
    @Override
    public PageResponse<?> getAlls(int pageNumber, int pageSize) {
        log.info("[THEATER SERVICE] Fetching all theaters - Page: {}, Size: {}", pageNumber, pageSize);

        if (pageNumber < 0 || pageSize < 0) {
            throw new BadRequestException("Invalid PageNumber and PageSize");
        }

        Page<TheaterResponse> theaterResponses = theaterRepository.findAll(PageRequest.of(pageNumber, pageSize))
                .map(TheaterMapper::toTheaterResponse);
        return PageResponse.of(theaterResponses);
    }

    /**
     * Get movies by theater and date
     *
     * @param theaterId theater id
     * @param date      date to filter movies
     * @return list of movie responses
     */
    @Cacheable(value = "theater:movies", key = "#theaterId + ':' + #date")
    @Override
    public List<MovieResponse> getMoviesByTheater(Long theaterId, LocalDate date) {
        log.info("[THEATER SERVICE] Fetching movies for theaterId={} on date={}", theaterId, date);

        if (date == null) {
            date = LocalDate.now();
        }

        if (!theaterRepository.existsById(theaterId)) {
            throw new NotFoundException("Theater not found with ID: " + theaterId);
        }

        List<Movie> movies = showtimeRepository.findMoviesByTheaterAndDate(theaterId, date);
        log.info("[DEBUG] Found {} movies for theaterId={}, date={}, dateType={}",
                movies.size(), theaterId, date, date.getClass().getSimpleName());
        if (movies.isEmpty()) {
            log.info("[THEATER SERVICE] No movies found for theater {}", theaterId);
        }

        return movies.stream()
                .map(MovieMapper::toMovieResponse)
                .toList();
    }

    /**
     * Count total number of theaters
     *
     * @return total count of theaters
     */
    @Override
    public long countTheaters() {
        log.info("[THEATER SERVICE] Counting theater information");
        return theaterRepository.count();
    }

    //=========================================== PRIVATE METHOD ===========================================//
    private void checkExistsAndUpdateField(UpdateTheaterRequest request, Theater theater) {
        if (request.getName() != null && !request.getName().equals(theater.getName())) {
            if (theaterRepository.existsByName(request.getName())) {
                log.error("[THEATER SERVICE] - [UPDATE] Theater with name '{}' already exists", request.getName());
                throw new BadRequestException("Theater with the same name already exists");
            }
            theater.setName(request.getName());
        }

        if (request.getLocation() != null) {
            theater.setLocation(request.getLocation());
        }

        if (request.getCity() != null) {
            theater.setCity(request.getCity());
        }

        if (request.getPhone() != null) {
            theater.setPhone(request.getPhone());
        }

        if (request.getStatus() != null) {
            theater.setStatus(request.getStatus());
        }
    }

    private Theater getTheaterById(Long theaterId) {
        return theaterRepository.findById(theaterId)
                .orElseThrow(() -> {
                    log.error("[THEATER SERVICE] Theater with ID '{}' not found", theaterId);
                    return new BadRequestException("Theater not found");
                });
    }

    private static Theater buildTheater(TheaterRequest request) {
        return Theater.builder()
                .name(request.getName())
                .location(request.getLocation())
                .city(request.getCity())
                .phone(request.getPhone())
                .status(TheaterStatus.INACTIVE)
                .build();
    }
}

