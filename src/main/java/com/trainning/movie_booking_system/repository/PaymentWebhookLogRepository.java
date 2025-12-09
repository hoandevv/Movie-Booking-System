package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.PaymentWebhookLog;
import com.trainning.movie_booking_system.utils.enums.PaymentGatewayType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, Long> {

    /**
     * Find all webhook logs by gateway type
     */
    Page<PaymentWebhookLog> findByGatewayType(PaymentGatewayType gatewayType, Pageable pageable);

    /**
     * Find all unprocessed webhook logs
     */
    @Query("SELECT pwl FROM PaymentWebhookLog pwl WHERE pwl.processed = false ORDER BY pwl.receivedAt ASC")
    List<PaymentWebhookLog> findUnprocessedLogs();

    /**
     * Find logs with invalid signatures (security monitoring)
     */
    @Query("SELECT pwl FROM PaymentWebhookLog pwl WHERE pwl.signatureValid = false")
    Page<PaymentWebhookLog> findInvalidSignatureLogs(Pageable pageable);

    /**
     * Find webhook logs for a specific payment transaction
     */
    @Query("SELECT pwl FROM PaymentWebhookLog pwl WHERE pwl.paymentTransaction.id = :transactionId ORDER BY pwl.receivedAt DESC")
    List<PaymentWebhookLog> findByPaymentTransactionId(@Param("transactionId") Long transactionId);

    /**
     * Find logs received within a time range
     */
    @Query("SELECT pwl FROM PaymentWebhookLog pwl WHERE pwl.receivedAt BETWEEN :startTime AND :endTime")
    List<PaymentWebhookLog> findLogsBetweenTimes(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * Count failed webhook processing attempts
     */
    @Query("SELECT COUNT(pwl) FROM PaymentWebhookLog pwl WHERE pwl.processed = true AND pwl.processingError IS NOT NULL")
    long countFailedProcessing();

    /**
     * Delete old webhook logs (cleanup - older than retention period)
     */
    @Query("DELETE FROM PaymentWebhookLog pwl WHERE pwl.receivedAt < :cutoffDate")
    void deleteOldLogs(@Param("cutoffDate") LocalDateTime cutoffDate);
}
