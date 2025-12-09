package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.response.Booking.BookingResponse;
import com.trainning.movie_booking_system.dto.response.Booking.BookingSeatResponse;
import com.trainning.movie_booking_system.dto.response.Seat.SeatBookingResponse;
import com.trainning.movie_booking_system.entity.Booking;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .status(booking.getStatus())
                .totalPrice(booking.getTotalPrice())
                .discountAmount(booking.getDiscountAmount())
                .finalAmount(booking.getFinalAmount())
                .bookingDate(booking.getBookingDate())
                .showtimeId(booking.getShowtime().getId())
                .voucherId(booking.getVoucher() != null ? booking.getVoucher().getId() : null)
                .voucherCode(booking.getVoucher() != null ? booking.getVoucher().getCode() : null)
                .accountId(booking.getAccount().getId())
                .seats(booking.getBookingSeats().stream()
                        .map(this::toSeatResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    public BookingSeatResponse toSeatResponse(com.trainning.movie_booking_system.entity.BookingSeat bookingSeat) {
        return BookingSeatResponse.builder()
                .id(bookingSeat.getId())
                .seat(SeatBookingResponse.builder()
                        .id(bookingSeat.getSeat().getId())
                        .seatNumber(bookingSeat.getSeat().getSeatNumber())
                        .rowLabel(bookingSeat.getSeat().getRowLabel())
                        .seatType(bookingSeat.getSeat().getSeatType())
                        .build())
                .price(bookingSeat.getPrice())
                .build();
    }
}
