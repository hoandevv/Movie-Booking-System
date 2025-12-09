package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.BookingSeat;
import com.trainning.movie_booking_system.utils.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    /**
     * Delete all BookingSeat entries associated with a specific booking ID.
     *
     * @param id the ID of the booking whose seats are to be deleted
     */
    void deleteByBookingId(Long id);

    /**
     * Find the IDs of seats that are already booked for a specific showtime and booking statuses.
     *
     * @param showtimeId the ID of the showtime
     * @param statuses   the list of booking statuses to consider
     * @param seatIds    the list of seat IDs to check
     * @return a list of seat IDs that are already booked
     */
    @Query("""
        select bs.seat.id
        from BookingSeat bs
        join bs.booking b
        where b.showtime.id = :showtimeId
          and b.status in :statuses
          and bs.seat.id in :seatIds
    """)
    List<Long> findBookedSeatIds(
            @Param("showtimeId") Long showtimeId,
            @Param("statuses") List<BookingStatus> statuses,
            @Param("seatIds") List<Long> seatIds
    );
}
