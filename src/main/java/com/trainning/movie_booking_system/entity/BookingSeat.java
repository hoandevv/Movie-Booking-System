package com.trainning.movie_booking_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "booking_seats",
        indexes = {
                @Index(name = "idx_booking", columnList = "booking_id"),
                @Index(name = "idx_booking_seat", columnList = "seat_id"),
                @Index(name = "idx_seat_info", columnList = "row_label, seat_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;
    
    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private com.trainning.movie_booking_system.utils.enums.SeatType seatType;
}
