package com.trainning.movie_booking_system.helper.cron;

import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.helper.redis.SeatDomainService;
import com.trainning.movie_booking_system.repository.BookingRepository;
import com.trainning.movie_booking_system.utils.enums.BookingStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cron job tự động expire các booking PENDING_PAYMENT quá hạn
 * Chạy mỗi 5 phút để release seats cho user khác
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BookingExpireService {

    private final BookingRepository bookingRepository;
    private final SeatDomainService seatDomainService;

    /**
     * Cron job chạy mỗi 5 phút để kiểm tra các booking đã hết hạn
     * Booking PENDING_PAYMENT có expiresAt < now sẽ bị EXPIRED
     */
    @Transactional
    @Scheduled(cron = "0 */5 * * * *")  // Chạy mỗi 5 phút
    public void expireBookings() {
        log.info("[BOOKING-EXPIRE] Starting booking expiration check...");

        // Lấy danh sách booking PENDING_PAYMENT đã hết hạn (expiresAt < now)
        List<Booking> expiredBookings = bookingRepository.findAllExpiredBookings(
                BookingStatus.PENDING_PAYMENT,
                LocalDateTime.now()
        );

        if (expiredBookings.isEmpty()) {
            log.info("[BOOKING-EXPIRE] No expired bookings found");
            return;
        }

        log.info("[BOOKING-EXPIRE] Found {} expired bookings", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            try {
                // 1. Update status = EXPIRED (KHÔNG XÓA booking_seats để audit)
                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);

                // 2. Release Redis hold (nếu còn) để user khác có thể đặt
                List<Long> seatIds = booking.getBookingSeats().stream()
                        .map(bs -> bs.getSeat().getId())
                        .toList();

                seatDomainService.releaseHolds(booking.getShowtime().getId(), seatIds);

                log.info("[BOOKING-EXPIRE] Booking ID {} expired, {} seats released",
                        booking.getId(), seatIds.size());

            } catch (Exception e) {
                log.error("[BOOKING-EXPIRE] Error expiring booking ID {}: {}",
                        booking.getId(), e.getMessage(), e);
            }
        }

        log.info("[BOOKING-EXPIRE] Completed. Expired {} bookings", expiredBookings.size());
    }
}
