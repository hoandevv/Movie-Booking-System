package com.trainning.movie_booking_system.dto.request.Seat;

import com.trainning.movie_booking_system.utils.enums.SeatType;
import lombok.Data;

import java.util.List;

@Data
public class SeatGenerationRequest {
    private int totalRows = 10;            // Tổng số hàng ghế
    private int seatsPerRow = 20;          // Số ghế mỗi hàng
    private List<String> vipRows;          // Danh sách các hàng VIP (ví dụ: ["A", "B"])
    private List<String> coupleRows;       // Danh sách các hàng ghế đôi
    private SeatType defaultSeatType = SeatType.STANDARD;  // Loại ghế mặc định
}
