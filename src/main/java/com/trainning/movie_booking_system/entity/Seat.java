package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.utils.enums.SeatStatus;
import com.trainning.movie_booking_system.utils.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import java.util.Objects;

@Entity
@Table(
        name = "seats",
        indexes = {
                @Index(name = "idx_seat_screen", columnList = "screen_id"),
                @Index(name = "idx_seat_row_seat", columnList = "row_label, seat_number")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_screen_row_seat",
                        columnNames = {"screen_id", "row_label", "seat_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE seats SET is_deleted = true WHERE id = ?") //  soft delete
@Where(clause = "is_deleted = false") //  chỉ lấy ghế chưa xóa
public class Seat extends BaseEntity {

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private SeatType seatType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeatStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "screen_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_seat_screen")
    )
    private Screen screen;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seat)) return false;
        Seat seat = (Seat) o;
        return seatNumber == seat.seatNumber &&
                Objects.equals(rowLabel, seat.rowLabel) &&
                Objects.equals(screen, seat.screen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seatNumber, rowLabel, screen);
    }
}
