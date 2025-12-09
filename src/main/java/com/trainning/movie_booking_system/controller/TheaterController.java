package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Theater.TheaterRequest;
import com.trainning.movie_booking_system.dto.request.Theater.UpdateTheaterRequest;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.service.TheaterService;
import com.trainning.movie_booking_system.utils.enums.TheaterStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/theaters")
@RequiredArgsConstructor
@Validated
@Slf4j
public class TheaterController {

    private final TheaterService theaterService;

    /**
     * Create a new theater
     *
     * @param request theater request object
     * @return theater response object
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid TheaterRequest request) {
        log.info("[THEATER-CONTROLLER] Create theater request: {}", request);
        return ResponseEntity.ok(BaseResponse.success(theaterService.create(request)));
    }

    /**
     * Update an existing theater
     *
     * @param theaterId theater id
     * @param request   theater request object
     * @return theater response object
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @PatchMapping("/{theaterId}")
    public ResponseEntity<?> update(@PathVariable Long theaterId, @RequestBody @Valid UpdateTheaterRequest request) {
        log.info("[THEATER-CONTROLLER] Update theater request: {}, {}", theaterId, request);
        return ResponseEntity.ok(BaseResponse.success(theaterService.update(theaterId, request)));
    }

    /**
     * Delete a theater
     *
     * @param theaterId theater id
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{theaterId}")
    public ResponseEntity<?> delete(@PathVariable Long theaterId, @RequestParam TheaterStatus status) {
        log.info("[THEATER-CONTROLLER] Delete theater request: {}", theaterId);
        theaterService.delete(theaterId, status);
        return ResponseEntity.ok(BaseResponse.success());
    }

    /**
     * Get a theater by id
     *
     * @param theaterId theater id
     * @return theater response object
     */
    @GetMapping("/{theaterId}")
    public ResponseEntity<?> getById(@PathVariable Long theaterId) {
        log.info("[THEATER-CONTROLLER] Get theater by id request: {}", theaterId);
        return ResponseEntity.ok(BaseResponse.success(theaterService.getById(theaterId)));
    }

    /**
     * Get all theaters with pagination
     *
     * @param pageNumber page number
     * @param pageSize   page size
     * @return paginated theater response
     */
    @GetMapping
    public ResponseEntity<?> getAlls(@RequestParam(defaultValue = "0") int pageNumber,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        log.info("[THEATER-CONTROLLER] Get all theaters request: {}, {}", pageNumber, pageSize);
        return ResponseEntity.ok(BaseResponse.success(theaterService.getAlls(pageNumber, pageSize)));
    }

    /**
     * Get movies by theater and date
     *
     * @param theaterId theater id
     * @param date      date to filter movies
     * @return list of movie responses
     */
    @GetMapping("/{theaterId}/movies")
    public ResponseEntity<?> getMoviesByTheater(
            @PathVariable Long theaterId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        log.info("[THEATER CONTROLLER] Get movies for theaterId={} date={}", theaterId, date);
        return ResponseEntity.ok(BaseResponse.success(
                theaterService.getMoviesByTheater(theaterId, date)
        ));
    }

    /**
     * Count total number of theaters
     *
     * @return total count of theaters
     */
    @GetMapping("/count")
    public ResponseEntity<?> countTheaters() {
        log.info("[THEATER-CONTROLLER] Count theaters request");
        return ResponseEntity.ok(BaseResponse.success(theaterService.countTheaters()));
    }
}
