package com.trainning.movie_booking_system.helper.redis;

import com.trainning.movie_booking_system.dto.SeatInfo;
import com.trainning.movie_booking_system.entity.Seat;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.ConflictException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.repository.BookingSeatRepository;
import com.trainning.movie_booking_system.repository.SeatRepository;
import com.trainning.movie_booking_system.utils.enums.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.trainning.movie_booking_system.repository.ShowtimeRepository;

/**
 * Service quản lý hold/release seats sử dụng Redis và DB đồng bộ trạng thái ghế
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatDomainService {

    private final StringRedisTemplate redis;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingSeatRepository bookingSeatRepository;

    private String holdKey(Long showtimeId, Long seatId) {
        return "hold:%d:%d".formatted(showtimeId, seatId);
    }
    private void validateShowtime(Long showtimeId) {
        var showtimeOpt = showtimeRepository.findById(showtimeId);
        if (showtimeOpt.isEmpty()) {
            throw new NotFoundException("Showtime not found: " + showtimeId);
        }

        var showtime = showtimeOpt.get();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime showtimeStart = LocalDateTime.of(showtime.getShowDate(), showtime.getStartTime());

        if (showtimeStart.isBefore(now)) {
            throw new BadRequestException("Cannot hold seat. Showtime %s has already started.".formatted(showtimeStart));
        }

        LocalDateTime cutoffTime = showtimeStart.minusMinutes(15); 
        if (now.isAfter(cutoffTime)) {
            throw new BadRequestException("Cannot hold seat. Booking closes 15 minutes before showtime. Cutoff: %s".formatted(cutoffTime));
        }
    }

    /**
     * Hold seats ATOMIC using Redis. 
     * Availability is checked against the specific showtime in DB and Redis.
     */
    public void holdSeats(Long showtimeId, List<Long> seatIds, Long userId, Duration ttl) {
        validateShowtime(showtimeId);
        log.info("[SEAT-HOLD] User {} attempting to hold seats {} for showtime {}", userId, seatIds, showtimeId);

        // 1. Check if seats are already booked for this showtime in DB
        List<Long> bookedInDb = bookingSeatRepository.findBookedSeatIds(
                showtimeId, 
                List.of(com.trainning.movie_booking_system.utils.enums.BookingStatus.PENDING_PAYMENT, 
                        com.trainning.movie_booking_system.utils.enums.BookingStatus.CONFIRMED), 
                seatIds);
        
        if (!bookedInDb.isEmpty()) {
            throw new ConflictException("Ghế %s đã được đặt hoặc đang trong quá trình thanh toán.".formatted(bookedInDb));
        }

        List<Long> heldSeats = new ArrayList<>();
        try {
            for (Long seatId : seatIds) {
                // Check physical availability (e.g. not under maintenance)
                Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new NotFoundException("Seat not found: " + seatId));

                if (seat.getStatus() == SeatStatus.MAINTENANCE) {
                    throw new ConflictException("Ghế %d hiện đang bảo trì.".formatted(seatId));
                }

                String key = holdKey(showtimeId, seatId);
                String userIdStr = String.valueOf(userId);

                // Atomic check-and-set in Redis
                Boolean success = redis.opsForValue().setIfAbsent(key, userIdStr, ttl);
                if (Boolean.TRUE.equals(success)) {
                    heldSeats.add(seatId);
                    log.debug("[SEAT-HOLD] Seat {} held in Redis", seatId);
                } else {
                    String currentOwner = redis.opsForValue().get(key);
                    if (userIdStr.equals(currentOwner)) {
                        redis.expire(key, ttl);
                        heldSeats.add(seatId);
                        log.debug("[SEAT-HOLD] Seat {} hold refreshed in Redis", seatId);
                    } else {
                        throw new ConflictException("Ghế %d đang được người khác giữ.".formatted(seatId));
                    }
                }
            }
            log.info("[SEAT-HOLD] Successfully held {} seats in Redis for user {}", heldSeats.size(), userId);
        } catch (Exception e) {
            log.error("[SEAT-HOLD] Failed to hold seats in Redis, rolling back", e);
            heldSeats.forEach(seatId -> redis.delete(holdKey(showtimeId, seatId)));
            throw e;
        }
    }

    /**
     * Release Redis holds
     */
    public void releaseHolds(Long showtimeId, List<Long> seatIds) {
        log.info("[SEAT-HOLD] Releasing Redis hold for {} seats", seatIds.size());
        seatIds.forEach(id -> {
            redis.delete(holdKey(showtimeId, id));
            log.debug("[SEAT-HOLD] Released Redis hold for seat {}", id);
        });
    }

    /**
     * Verify seats held by user in Redis
     */
    public void assertHeldByUser(Long showtimeId, List<Long> seatIds, Long userId) {
        for (Long seatId : seatIds) {
            String key = holdKey(showtimeId, seatId);
            String owner = redis.opsForValue().get(key);
            if (owner == null)
                throw new ConflictException("Ghế %d không còn được giữ (timeout).".formatted(seatId));
            if (!owner.equals(String.valueOf(userId)))
                throw new ConflictException("Ghế %d đang được người khác giữ.".formatted(seatId));
        }
    }

    /**
     * Clear Redis hold after successful booking creation (or payment)
     */
    public void consumeHoldToBooked(Long showtimeId, List<Long> seatIds) {
        log.info("[SEAT-HOLD] Clearing Redis hold for {} booked seats", seatIds.size());
        seatIds.forEach(id -> {
            redis.delete(holdKey(showtimeId, id));
        });
    }

    /**
     * Lấy thông tin ghế từ DB (seatType)
     */
    public List<SeatInfo> getSeatInfos(List<Long> seatIds) {
        var seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            List<Long> foundIds = seats.stream().map(Seat::getId).toList();
            List<Long> missingIds = seatIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BadRequestException("Ghế không tồn tại: %s".formatted(missingIds));
        }
        return seats.stream().map(s -> new SeatInfo(s.getId(), s.getSeatType())).toList();
    }

    /**
     * Kiểm tra ghế có đang bị hold không
     */
    public boolean isSeatHeld(Long showtimeId, Long seatId) {
        return Boolean.TRUE.equals(redis.hasKey(holdKey(showtimeId, seatId)));
    }
}
