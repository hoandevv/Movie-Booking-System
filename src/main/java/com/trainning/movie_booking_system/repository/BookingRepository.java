package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.utils.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    /**
     * Find all bookings that have expired based on status and expiry time.
     * @param status the booking status to filter by
     * @param expiryTime the expiry time to compare booking dates against
     * @return a list of expired bookings
     */
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.bookingDate < :expiryTime")
    List<Booking> findAllExpiredBookings(@Param("status") BookingStatus status,
                                         @Param("expiryTime") LocalDateTime expiryTime);

    /**
     * Fetch booking with its bookingSeats and seat entities to avoid N+1.
     */
    @Query("""
        select b from Booking b
        left join fetch b.bookingSeats bs
        left join fetch bs.seat s
        where b.id = :id
    """)
    Optional<Booking> findByIdWithSeats(@Param("id") Long id);

    /**
     * Find all bookings by their status.
     * @param status the booking status to filter by
     *               @return a list of bookings with the specified status
     * */
    List<Booking> findAllByStatus(BookingStatus status);
}
