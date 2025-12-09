package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Showtime.ShowtimeRequest;
import com.trainning.movie_booking_system.dto.request.Showtime.UpdateShowtimeRequest;
import com.trainning.movie_booking_system.dto.response.Showtime.ShowtimeByScreenResponse;
import com.trainning.movie_booking_system.dto.response.Showtime.ShowtimeResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.entity.Movie;
import com.trainning.movie_booking_system.entity.Screen;
import com.trainning.movie_booking_system.entity.Showtime;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.mapper.ShowtimeMapper;
import com.trainning.movie_booking_system.repository.MovieRepository;
import com.trainning.movie_booking_system.repository.ScreenRepository;
import com.trainning.movie_booking_system.repository.ShowtimeRepository;
import com.trainning.movie_booking_system.service.ShowtimeService;
import com.trainning.movie_booking_system.utils.enums.ShowtimeStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static com.trainning.movie_booking_system.mapper.ShowtimeMapper.toShowtimeResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final ScreenRepository screenRepository;
    private final MovieRepository movieRepository;

    /**
     * Create a new showtime.
     *
     * @param request the showtime request data
     * @return the created showtime response
     */
    @Transactional
    @Override
    public ShowtimeResponse create(ShowtimeRequest request) {
        log.info("[SHOWTIME SERVICE]: Creating new showtime with request: {}", request);

        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> {
                    log.error("[SHOWTIME SERVICE]: Screen with ID {} not found", request.getScreenId());
                    return new BadRequestException("Screen not found");
                });

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> {
                    log.error("[SHOWTIME SERVICE]: Movie with ID {} not found", request.getMovieId());
                    return new BadRequestException("Movie not found");
                });

        if (showtimeRepository.existsByScreenIdAndShowDateAndStartTime(request.getScreenId(),
                request.getShowDate(), request.getStartTime())) {

            log.error("[SHOWTIME SERVICE]: Showtime already exists for screen ID {}, date {}, and start time {}",
                    request.getScreenId(), request.getShowDate(), request.getStartTime());
            throw new BadRequestException("Showtime already exists for the given screen, date, and time");
        }

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .screen(screen)
                .showDate(request.getShowDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(ShowtimeStatus.ACTIVE)
                .build();
        showtimeRepository.save(showtime);

        return toShowtimeResponse(showtime);
    }

    /**
     * Update an existing showtime.
     *
     * @param id      the ID of the showtime to update
     * @param request the updated showtime request data
     * @return the updated showtime response
     */
    @Transactional
    @Override
    public ShowtimeResponse update(Long id, UpdateShowtimeRequest request) {
        log.info("[SHOWTIME SERVICE]: Updating showtime ID {} with data: {}", id, request);

        Showtime showtime = getShowtime(id);

        // Kiểm tra screen nếu có thay đổi
        if (request.getScreenId() != null && !request.getScreenId().equals(showtime.getScreen().getId())) {
            Screen newScreen = screenRepository.findById(request.getScreenId())
                    .orElseThrow(() -> new NotFoundException("Screen not found with ID: " + request.getScreenId()));
            showtime.setScreen(newScreen);
        }

        if (request.getMovieId() != null && !request.getMovieId().equals(showtime.getMovie().getId())) {
            Movie newMovie = movieRepository.findById(request.getMovieId())
                    .orElseThrow(() -> new NotFoundException("Movie not found with ID: " + request.getMovieId()));
            showtime.setMovie(newMovie);
        }

        // Kiểm tra trùng lịch (nếu có thay đổi ngày/giờ/screen)
        if (request.getShowDate() != null || request.getStartTime() != null) {
            Long screenId = request.getScreenId() != null ? request.getScreenId() : showtime.getScreen().getId();
            var date = request.getShowDate() != null ? request.getShowDate() : showtime.getShowDate();
            var startTime = request.getStartTime() != null ? request.getStartTime() : showtime.getStartTime();

            boolean exists = showtimeRepository.existsByScreenIdAndShowDateAndStartTimeAndIdNot(
                    screenId, date, startTime, id);
            if (exists) {
                throw new BadRequestException("Another showtime already exists for this screen/date/time");
            }
        }

        // Update các trường có thay đổi
        if (request.getShowDate() != null) showtime.setShowDate(request.getShowDate());
        if (request.getStartTime() != null) showtime.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) showtime.setEndTime(request.getEndTime());
        if (request.getPrice() != null) showtime.setPrice(request.getPrice());
        if (request.getStatus() != null) showtime.setStatus(request.getStatus());

        showtimeRepository.save(showtime);
        return toShowtimeResponse(showtime);
    }

    /**
     * Delete a showtime by its ID.
     *
     * @param id the ID of the showtime to delete
     */
    @Override
    public void delete(Long id, ShowtimeStatus status) {
        log.info("[SHOWTIME SERVICE]: Deleting showtime ID {}", id);
        Showtime showtime = getShowtime(id);
        showtime.setStatus(status);
        showtimeRepository.save(showtime);
    }

    /**
     * Get a showtime by its ID.
     *
     * @param id the ID of the showtime to retrieve
     * @return the showtime response
     */
    @Override
    public ShowtimeResponse getById(Long id) {
        log.info("[SHOWTIME SERVICE]: Retrieving showtime ID {}", id);
        Showtime showtime = getShowtime(id);
        return toShowtimeResponse(showtime);
    }

    /**
     * Get all showtimes with pagination.
     *
     * @param pageNumber the page number to retrieve
     * @param pageSize   the number of showtimes per page
     * @return a paginated response of showtimes
     */
    @Override
    public PageResponse<?> getAll(int pageNumber, int pageSize) {
        log.info("[SHOWTIME SERVICE]: Retrieving all showtimes - Page: {}, Size: {}", pageNumber, pageSize);

        if (pageNumber < 0 || pageSize < 0) {
            throw new BadRequestException("Invalid PageNumber and PageSize");
        }

        Page<ShowtimeResponse> showtimeResponses = showtimeRepository.findAll(PageRequest.of(pageNumber, pageSize))
                .map(ShowtimeMapper::toShowtimeResponse);
        return PageResponse.of(showtimeResponses);
    }

    /**
     * Get showtimes by theater and movie on a specific date.
     *
     * @param theaterId the ID of the theater
     * @param movieId   the ID of the movie
     * @param date      the date for which to retrieve showtimes
     * @return a list of showtimes grouped by screen
     */
    @Cacheable(value = "showtimes:theater-movie", key = "#theaterId + ':' + #movieId + ':' + #date")
    @Override
    public List<ShowtimeByScreenResponse> findByTheaterAndMovie(Long theaterId, Long movieId, LocalDate date) {
        log.info("[SHOWTIME SERVICE] Get showtimes for theater={}, movie={}, date={}", theaterId, movieId, date);

        if (date == null) {
            date = LocalDate.now();
        }

        List<Showtime> showtimes = showtimeRepository.findShowtimesByTheaterAndMovieAndDate(theaterId, movieId, date);
        if (showtimes.isEmpty()) {
            log.warn("[SHOWTIME SERVICE] No showtimes found for theater={}, movie={}, date={}", theaterId, movieId, date);
            return List.of();
        }

        // Nhóm theo phòng chiếu
        Map<String, List<ShowtimeByScreenResponse.ShowtimeSlotResponse>> grouped = showtimes.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getScreen().getName(),
                        LinkedHashMap::new,
                        Collectors.mapping(s -> ShowtimeByScreenResponse.ShowtimeSlotResponse.builder()
                                        .id(s.getId())
                                        .startTime(s.getStartTime().toString())
                                        .endTime(s.getEndTime().toString())
                                        .price(s.getPrice().doubleValue())
                                        .build(),
                                Collectors.toList())
                ));

        // Trả về list
        return grouped.entrySet().stream()
                .map(e -> ShowtimeByScreenResponse.builder()
                        .screenName(e.getKey())
                        .showtimes(e.getValue())
                        .build())
                .toList();
    }

    /**
     * Count the total number of showtimes.
     *
     * @return the total count of showtimes
     */
    @Override
    public long countShowtime() {
        log.info("[SHOWTIME SERVICE]: Counting total showtimes");
        return showtimeRepository.count();
    }

    //====================== PRIVATE METHOD ====================//
    private Showtime getShowtime(Long id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Showtime not found with ID: " + id));
    }
}
