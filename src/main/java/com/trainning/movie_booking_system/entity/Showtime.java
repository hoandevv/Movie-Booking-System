package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.utils.enums.ShowtimeStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "showtimes",
        indexes = {
                @Index(name = "idx_showtime_movie_id", columnList = "movie_id"),
                @Index(name = "idx_showtime_screen_id", columnList = "screen_id"),
                @Index(name = "idx_showtime_date", columnList = "show_date"),
                @Index(name = "idx_showtime_start_time", columnList = "start_time"),
                @Index(name = "idx_showtime_end_time", columnList = "end_time"),
                @Index(name = "idx_showtime_price", columnList = "price"),
                @Index(name = "idx_showtime_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_screen_movie_time", columnNames = {"screen_id", "show_date", "start_time"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Showtime extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ShowtimeStatus status;
}
