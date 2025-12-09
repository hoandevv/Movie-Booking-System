package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.utils.enums.ScreenStatus;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "screens",
        indexes = {
                @Index(name = "idx_screen_name", columnList = "name"),
                @Index(name = "idx_screen_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_theater_screen_name", columnNames = {"theater_id", "name"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Screen extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Integer totalSeats;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ScreenStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Seat> seats = new HashSet<>();

    public void addSeat(Seat seat) {
        seats.add(seat);
        seat.setScreen(this);
        this.totalSeats = seats.size();
    }

    public void removeSeat(Seat seat) {
        seats.remove(seat);
        seat.setScreen(null);
        this.totalSeats = seats.size();
    }
}
