package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Seat.SeatGenerationRequest;
import com.trainning.movie_booking_system.dto.request.Seat.SeatRequest;
import com.trainning.movie_booking_system.dto.response.Seat.SeatResponse;
import com.trainning.movie_booking_system.entity.Screen;
import com.trainning.movie_booking_system.entity.Seat;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.mapper.SeatMapper;
import com.trainning.movie_booking_system.repository.ScreenRepository;
import com.trainning.movie_booking_system.repository.SeatRepository;
import com.trainning.movie_booking_system.service.SeatService;
import com.trainning.movie_booking_system.utils.enums.ScreenStatus;
import com.trainning.movie_booking_system.utils.enums.SeatStatus;
import com.trainning.movie_booking_system.utils.enums.SeatType;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;
    private final SeatMapper seatMapper;

    /**
     * Create a new seat
     *
     * @param request seat request object
     * @return seat response object
     */
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "seats:screen", key = "#request.screenId"),
            @CacheEvict(value = "seat:detail", allEntries = true)
    })
    public SeatResponse create(SeatRequest request) {
        log.info("[SEAT-SERVICE] Create seat request: {}", request);

        // Check if seat already exists
        if (seatRepository.existsByScreenIdAndRowLabelAndSeatNumber(
                request.getScreenId(),
                request.getRowLabel(),
                request.getSeatNumber())) {
            throw new BadRequestException(
                    String.format("Seat %s-%s already exists in screen ID %d",
                            request.getRowLabel(),
                            request.getSeatNumber(),
                            request.getScreenId())
            );
        }

        // Get screen
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Screen not found with id: %d", request.getScreenId())
                ));

        // Create seat
        Seat seat = seatMapper.toEntity(request);
        seat.setScreen(screen);

        // Save seat
        Seat savedSeat = seatRepository.save(seat);
        log.info("[SEAT-SERVICE] Seat created successfully with id: {}", savedSeat.getId());

        return seatMapper.toResponse(savedSeat);
    }

    /**
     * Update an existing seat
     *
     * @param seatId  seat id
     * @param request seat request object
     * @return seat response object
     */
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "seats:screen", allEntries = true),
            @CacheEvict(value = "seat:detail", key = "#seatId")
    })
    public SeatResponse update(Long seatId, SeatRequest request) {
        log.info("[SEAT-SERVICE] Update seat request: {}, {}", seatId, request);

        // Get seat
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Seat not found with id: %d", seatId)
                ));

        // Check if updating to a seat number that already exists
        if (request.getRowLabel() != null && request.getSeatNumber() != null) {
            if (!seat.getRowLabel().equals(request.getRowLabel()) ||
                    seat.getSeatNumber() != request.getSeatNumber()) {
                if (seatRepository.existsByScreenIdAndRowLabelAndSeatNumber(
                        seat.getScreen().getId(),
                        request.getRowLabel(),
                        request.getSeatNumber())) {
                    throw new BadRequestException(
                            String.format("Seat %s-%s already exists in this screen",
                                    request.getRowLabel(),
                                    request.getSeatNumber())
                    );
                }
            }
        }

        // Update screen if provided
        if (request.getScreenId() != null && !seat.getScreen().getId().equals(request.getScreenId())) {
            Screen newScreen = screenRepository.findById(request.getScreenId())
                    .orElseThrow(() -> new NotFoundException(
                            String.format("Screen not found with id: %d", request.getScreenId())
                    ));
            seat.setScreen(newScreen);
        }

        // Update seat
        seatMapper.updateEntity(seat, request);

        // Save seat
        Seat updatedSeat = seatRepository.save(seat);
        log.info("[SEAT-SERVICE] Seat updated successfully with id: {}", updatedSeat.getId());

        return seatMapper.toResponse(updatedSeat);
    }

    /**
     * Soft delete a seat
     *
     * @param seatId seat id
     */
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "seats:screen", allEntries = true),
            @CacheEvict(value = "seat:detail", key = "#seatId")
    })
    public void delete(Long seatId) {
        log.info("[SEAT-SERVICE] Soft delete seat request: {}", seatId);

        // Lấy ghế
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Seat not found with id: %d", seatId)
                ));

        // Đánh dấu xóa mềm
        seat.setDeleted(true);
        seatRepository.save(seat);
        log.info("[SEAT-SERVICE] Seat soft deleted successfully with id: {}", seatId);
    }


    /**
     * Get seat by id
     *
     * @param seatId seat id
     * @return seat response object
     */
    @Override
    @Cacheable(value = "seat:detail", key = "#seatId")
    public SeatResponse getById(Long seatId) {
        log.info("[SEAT-SERVICE] Get seat by id request: {}", seatId);

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Seat not found with id: %d", seatId)
                ));

        return seatMapper.toResponse(seat);
    }

    /**
     * Get all seats with pagination
     *
     * @param pageNumber page number
     * @param pageSize   page size
     * @return paginated list of seat response objects
     */
    @Override
    public Page<SeatResponse> getAlls(int pageNumber, int pageSize) {
        log.info("[SEAT-SERVICE] Get all seats request: {}, {}", pageNumber, pageSize);

        Page<Seat> seats = seatRepository.findAll(PageRequest.of(pageNumber, pageSize));

        return seats.map(seatMapper::toResponse);
    }

    /**
     * Get all seats by screen ID
     *
     * @param screenId screen id
     * @return list of seat response objects
     */
    @Override
    @Cacheable(value = "seats:screen", key = "#screenId")
    public List<SeatResponse> getSeatsByScreenId(Long screenId) {
        log.info("[SEAT-SERVICE] Get seats by screen id request: {}", screenId);
        return getByScreenId(screenId);
    }

    /**
     * Generate seats automatically for a screen.
     *
     * @param screenId the ID of the screen to generate seats for
     * @return list of generated seats
     */
    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getByScreenId(Long screenId) {
        log.info("[SEAT-SERVICE] Get seats by screen ID: {}", screenId);
        
        // Check if screen exists
        if (!screenRepository.existsById(screenId)) {
            throw new NotFoundException("Screen not found with id: " + screenId);
        }

        return seatRepository.findAllByScreenIdOrderByRowLabelAndSeatNumber(screenId)
                .stream()
                .map(seatMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "seats:screen", key = "#screenId"),
            @CacheEvict(value = "seat:detail", allEntries = true)
    })
    public List<SeatResponse> generateSeats(Long screenId, SeatGenerationRequest request) {
        log.info("[SEAT-SERVICE] Generating seats for screen ID: {} with config: {}", screenId, request);

        //  1. Kiểm tra screen tồn tại
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new NotFoundException("Screen not found with id: " + screenId));

        //  2. Kiểm tra trạng thái screen
        if (screen.getStatus() == ScreenStatus.INACTIVE) {
            throw new BadRequestException("Không thể tạo ghế cho Screen đang INACTIVE");
        }

        //  3. Kiểm tra nếu đã có ghế
        if (seatRepository.existsByScreen_Id(screenId)) {
            throw new BadRequestException("Ghế cho screen này đã tồn tại. Vui lòng xóa hoặc reset trước khi tạo mới.");
        }

        //  4. Tính tổng số ghế sẽ tạo
        int totalSeatsToGenerate = request.getTotalRows() * request.getSeatsPerRow();

        //  5. Kiểm tra khớp với tổng ghế trong screen
        if (screen.getTotalSeats() != null && screen.getTotalSeats() > 0) {
            if (totalSeatsToGenerate != screen.getTotalSeats()) {
                throw new BadRequestException(String.format(
                        "Số ghế cần tạo (%d) không khớp với tổng ghế được cấu hình trong Screen (%d)",
                        totalSeatsToGenerate, screen.getTotalSeats()
                ));
            }
        }

        // 6. Tạo danh sách ghế
        List<Seat> seats = new ArrayList<>();
        char startRow = 'A';

        for (int i = 0; i < request.getTotalRows(); i++) {
            String rowLabel = String.valueOf((char) (startRow + i));

            for (int seatNumber = 1; seatNumber <= request.getSeatsPerRow(); seatNumber++) {
                Seat seat = Seat.builder()
                        .screen(screen)
                        .rowLabel(rowLabel)
                        .seatNumber(seatNumber)
                        .status(SeatStatus.AVAILABLE)
                        .seatType(determineSeatType(request, rowLabel))
                        .build();

                seats.add(seat);
            }
        }

        // 7. Lưu danh sách ghế
        List<Seat> savedSeats = seatRepository.saveAll(seats);

        // 8. Cập nhật tổng số ghế cho màn hình (nếu chưa có)
        if (screen.getTotalSeats() == null || screen.getTotalSeats() == 0) {
            screen.setTotalSeats(savedSeats.size());
            screenRepository.save(screen);
        }

        log.info("[SEAT-SERVICE] Generated {} seats successfully for screen ID: {}", savedSeats.size(), screenId);

        return savedSeats.stream()
                .map(seatMapper::toResponse)
                .collect(Collectors.toList());
    }
    /**
     * Xác định loại ghế dựa theo hàng.
     */
    private SeatType determineSeatType(SeatGenerationRequest request, String rowLabel) {
        if (request.getVipRows() != null && request.getVipRows().contains(rowLabel)) {
            return SeatType.VIP;
        }
        if (request.getCoupleRows() != null && request.getCoupleRows().contains(rowLabel)) {
            return SeatType.COUPLE;
        }
        return request.getDefaultSeatType() != null ? request.getDefaultSeatType() : SeatType.STANDARD;
    }
    
    @Override
    public List<SeatResponse> getSeatsByScreenIdAndStatus(Long screenId, SeatStatus status) {
        log.info("[SEAT-SERVICE] Get seats by screen ID: {} and status: {}", screenId, status);
        return seatRepository.findByScreenIdAndStatus(screenId, status)
                .stream()
                .map(seatMapper::toResponse)
                .collect(Collectors.toList());
    }
}
