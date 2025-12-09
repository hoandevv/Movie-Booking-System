package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Screen.ScreenRequest;
import com.trainning.movie_booking_system.dto.request.Screen.UpdateScreenRequest;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.service.ScreenService;
import com.trainning.movie_booking_system.utils.enums.ScreenStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/screens")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ScreenController {

    private final ScreenService service;

    /**
     * Create a new screen
     *
     * @param request screen request object
     * @return screen response object
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid ScreenRequest request) {
        log.info("[SCREEN-CONTROLLER] Create screen request: {}", request);
        return ResponseEntity.ok(BaseResponse.success(service.create(request)));
    }

    /**
     * Update an existing screen
     *
     * @param screenId screen id
     * @param request  screen request object
     * @return screen response object
     */
    @PatchMapping("/{screenId}")
    public ResponseEntity<?> update(@PathVariable Long screenId, @RequestBody @Valid UpdateScreenRequest request) {
        log.info("[SCREEN-CONTROLLER] Update screen request: {}, {}", screenId, request);
        return ResponseEntity.ok(BaseResponse.success(service.update(screenId, request)));
    }

    /**
     * Delete a screen
     *
     * @param screenId screen id
     */
    @DeleteMapping("/{screenId}")
    public ResponseEntity<?> delete(@PathVariable Long screenId, @RequestParam ScreenStatus status) {
        log.info("[SCREEN-CONTROLLER] Delete screen request: {}", screenId);
        service.delete(screenId, status);
        return ResponseEntity.ok(BaseResponse.success());
    }

    /**
     * Get a screen by id
     *
     * @return screen response object
     */
    @GetMapping("/{screenId}")
    public ResponseEntity<?> getScreenById(@PathVariable Long screenId) {
        log.info("[SCREEN-CONTROLLER] Get screen by id request: {}", screenId);
        return ResponseEntity.ok(BaseResponse.success(service.getById(screenId)));
    }

    /**
     * Get screens by theater id
     *
     * @param theaterId theater id
     * @return screen response object
     */
    @GetMapping("/theater/{theaterId}")
    public ResponseEntity<?> getByTheater(@PathVariable Long theaterId) {
        log.info("[SCREEN-CONTROLLER] Get screens by theater id request: {}", theaterId);
        return ResponseEntity.ok(BaseResponse.success(service.getByTheater(theaterId)));
    }

    /**
     * Get all screens with pagination
     *
     * @param pageNumber page number
     * @param pageSize   page size
     * @return paginated screen response
     */
    @GetMapping
    public ResponseEntity<?> getAlls(@RequestParam(defaultValue = "0") int pageNumber,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        log.info("[SCREEN-CONTROLLER] Get all screens request: pageNumber={}, pageSize={}", pageNumber, pageSize);
        return ResponseEntity.ok(BaseResponse.success(service.getAll(pageNumber, pageSize)));
    }

    /**
     * Count all screens
     *
     * @return total number of screens
     */
    @GetMapping("/count")
    public ResponseEntity<?> countAllScreens() {
        log.info("[SCREEN-CONTROLLER] Count all screens request");
        return ResponseEntity.ok(BaseResponse.success(service.countAllScreens()));
    }
}
