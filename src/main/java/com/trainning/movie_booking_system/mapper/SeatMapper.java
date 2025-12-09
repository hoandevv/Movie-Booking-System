package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.request.Seat.SeatRequest;
import com.trainning.movie_booking_system.dto.response.Seat.SeatResponse;
import com.trainning.movie_booking_system.entity.Seat;
import com.trainning.movie_booking_system.repository.ScreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeatMapper {

    private final ScreenRepository screenRepository;

    public SeatResponse toResponse(Seat seat) {
        if (seat == null) {
            return null;
        }

        return SeatResponse.builder()
                .id(seat.getId())
                .screenId(seat.getScreen().getId())
                .screenName(seat.getScreen().getName()) // Thêm dòng này để lấy tên màn hình
                .rowLabel(seat.getRowLabel())
                .seatNumber(seat.getSeatNumber())
                .seatType(seat.getSeatType())
                .status(seat.getStatus())
                .createdAt(seat.getCreatedAt())
                .updatedAt(seat.getUpdatedAt())
                .build();
    }

    public Seat toEntity(SeatRequest request) {
        if (request == null) {
            return null;
        }

        return Seat.builder()
                .rowLabel(request.getRowLabel())
                .seatNumber(request.getSeatNumber())
                .seatType(request.getSeatType())
                .status(request.getStatus())
                .screen(screenRepository.findById(request.getScreenId())
                        .orElseThrow(() -> new RuntimeException("Screen not found with id: " + request.getScreenId())))
                .build();
    }

    public void updateEntity(Seat seat, SeatRequest request) {
        if (seat == null || request == null) {
            return;
        }

        if (request.getRowLabel() != null && !request.getRowLabel().isBlank()) {
            seat.setRowLabel(request.getRowLabel());
        }

        if (request.getSeatNumber() != null && request.getSeatNumber() > 0) {
            seat.setSeatNumber(request.getSeatNumber());
        }

        if (request.getSeatType() != null) {
            seat.setSeatType(request.getSeatType());
        }

        if (request.getStatus() != null) {
            seat.setStatus(request.getStatus());
        }
        if (request.getScreenId() != null && 
            (seat.getScreen() == null || !request.getScreenId().equals(seat.getScreen().getId()))) {
            seat.setScreen(screenRepository.findById(request.getScreenId())
                    .orElseThrow(() -> new RuntimeException("Screen not found with id: " + request.getScreenId())));
        }
    }
}
