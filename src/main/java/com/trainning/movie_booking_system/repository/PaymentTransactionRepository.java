package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.entity.PaymentTransaction;
import com.trainning.movie_booking_system.utils.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Find transaction by transaction ID (from payment gateway)
     */
    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    /**
     * Find all transactions for a booking
     */
    List<PaymentTransaction> findByBookingId(Long bookingId);

    /**
     * Find transaction by booking ID and status
     */
    Optional<PaymentTransaction> findByBookingIdAndStatus(Long bookingId, PaymentStatus status);
    /**
     * Find the most recent transaction for a booking
     */
    Optional<PaymentTransaction> findTopByBookingIdOrderByCreatedAtDesc(Long bookingId);
    /**
     * Check if transaction ID already exists (for idempotency)
     */
    boolean existsByTransactionId(String transactionId);


    /**
     * Find all transactions by status
     */
    List<PaymentTransaction> findByStatus(PaymentStatus status);

    /**
     * Find transactions by booking ID and gateway type
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.booking.id = :bookingId AND pt.gatewayType = :gatewayType")
    List<PaymentTransaction> findByBookingIdAndGatewayType(@Param("bookingId") Long bookingId, 
                                                           @Param("gatewayType") String gatewayType);

    /**
     * Find pending transactions older than specified time (for cleanup)
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.status = :status AND pt.initiatedAt < :beforeTime")
    List<PaymentTransaction> findPendingTransactionsOlderThan(@Param("status") PaymentStatus status,
                                                               @Param("beforeTime") LocalDateTime beforeTime);

    /**
     * Count successful transactions for a user (for analytics)
     */
    @Query("SELECT COUNT(pt) FROM PaymentTransaction pt WHERE pt.booking.account.id = :userId AND pt.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") PaymentStatus status);
    /**
     * Lấy transaction mới nhất của booking theo initiatedAt
     */
    Optional<PaymentTransaction> findTopByBookingOrderByInitiatedAtDesc(Booking booking);

}
