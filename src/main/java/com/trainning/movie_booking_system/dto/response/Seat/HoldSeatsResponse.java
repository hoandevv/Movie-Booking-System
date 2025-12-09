package com.trainning.movie_booking_system.dto.response.Seat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * Response DTO cho việc hold seats
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class HoldSeatsResponse {
    private Long showtimeId;
    private List<Long> heldSeatIds;
    private Integer ttlSec; // thời gian hold
    private String message;
}
