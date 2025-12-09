package com.trainning.movie_booking_system.dto.response.Seat;

import com.trainning.movie_booking_system.utils.enums.SeatStatus;
import com.trainning.movie_booking_system.utils.enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private Long id;
    private Long screenId;
    private String screenName;
    private String rowLabel;
    private int seatNumber;
    private SeatType seatType;
    private SeatStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
