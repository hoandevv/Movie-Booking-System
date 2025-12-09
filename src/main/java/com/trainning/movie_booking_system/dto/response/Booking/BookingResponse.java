package com.trainning.movie_booking_system.dto.response.Booking;

import com.trainning.movie_booking_system.utils.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BookingResponse {
    private Long id;
    private Long accountId;
    private Long showtimeId;
    private Long voucherId;
    private String voucherCode;
    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private BookingStatus status;
    private LocalDateTime bookingDate;
    private List<BookingSeatResponse> seats;
}
