package com.trainning.movie_booking_system.helper.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis distributed lock service với UUID để tránh unlock nhầm
 * Hỗ trợ lock theo seat, tránh race condition
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;
    private final ThreadLocal<String> lockOwner = new ThreadLocal<>();

    /**
     * Thử lấy lock từ Redis với unique lock owner
     *
     * @param lockKey   Key để lock (ví dụ: "seat:lock:{showtimeId}:{seatId}")
     * @param ttl       Thời gian khóa tồn tại
     * @param timeUnit  Đơn vị thời gian
     * @return true nếu lấy được lock
     */
    public boolean tryLock(String lockKey, long ttl, TimeUnit timeUnit) {
        String lockValue = UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, ttl, timeUnit);

        if (Boolean.TRUE.equals(success)) {
            lockOwner.set(lockValue);
            log.debug("[REDIS-LOCK] Acquired lock: {}", lockKey);
            return true;
        }

        log.warn("[REDIS-LOCK] Failed to acquire lock: {}", lockKey);
        return false;
    }

    /**
     * Giải phóng lock - chỉ unlock nếu là owner
     */
    public void releaseLock(String lockKey) {
        try {
            String ownerValue = lockOwner.get();
            if (ownerValue != null) {
                String currentValue = redisTemplate.opsForValue().get(lockKey);
                if (ownerValue.equals(currentValue)) {
                    redisTemplate.delete(lockKey);
                    log.debug("[REDIS-LOCK] Released lock: {}", lockKey);
                } else {
                    log.warn("[REDIS-LOCK] Cannot release lock (not owner): {}", lockKey);
                }
            }
        } finally {
            lockOwner.remove();
        }
    }

    /**
     * Lock theo seat
     */
    public boolean tryLockSeat(Long showtimeId, Long seatId, long ttl, TimeUnit timeUnit) {
        String lockKey = "seat:lock:%d:%d".formatted(showtimeId, seatId);
        return tryLock(lockKey, ttl, timeUnit);
    }

    /**
     * Unlock seat
     */
    public void releaseSeatLock(Long showtimeId, Long seatId) {
        String lockKey = "seat:lock:%d:%d".formatted(showtimeId, seatId);
        releaseLock(lockKey);
    }

    /**
     * Optional: Retry lock với timeout
     */
    public boolean tryLockWithRetry(String lockKey, long ttl, TimeUnit timeUnit, int maxRetry, long sleepMillis) {
        for (int i = 0; i < maxRetry; i++) {
            if (tryLock(lockKey, ttl, timeUnit)) return true;
            try { Thread.sleep(sleepMillis); } catch (InterruptedException ignored) {}
        }
        return false;
    }
}
