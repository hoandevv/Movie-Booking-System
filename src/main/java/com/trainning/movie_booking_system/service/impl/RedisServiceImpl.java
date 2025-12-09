package com.trainning.movie_booking_system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainning.movie_booking_system.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Lưu key-value với TTL vào Redis
     *
     * @param key      Redis key
     * @param value    Redis value
     * @param ttl      Thời gian sống (timeout)
     * @param timeUnit Đơn vị thời gian (SECONDS, MINUTES, HOURS...)
     */
    @Override
    public void set(String key, Object value, long ttl, TimeUnit timeUnit) {
        log.info("Insert to Redis with key: {}", key);
        redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
    }

    /**
     * Lấy giá trị từ Redis theo key
     *
     * @param key Redis key
     * @return Object hoặc null nếu không tồn tại
     */
    @Override
    public Object get(String key) {
        log.info("Get value by key: {}", key);
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Lấy và convert về kiểu mong muốn
     *
     * @param key   Redis key
     * @param clazz Kiểu dữ liệu mong muốn
     * @return Optional<T>, empty nếu không có dữ liệu
     */
    @Override
    public <T> Optional<T> get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }

        if (clazz.isInstance(value)) {
            return Optional.of(clazz.cast(value));
        }

        if (value instanceof String str) {
            try {
                return Optional.of(objectMapper.readValue(str, clazz));
            } catch (Exception e) {
                log.error("Failed to parse value from Redis for key: {}", key, e);
            }
        }

        return Optional.empty();
    }

    /**
     * Kiểm tra key có tồn tại trong Redis hay không
     *
     * @param key Redis key
     * @return true nếu tồn tại, false nếu không
     */
    @Override
    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * Xóa key trong Redis
     *
     * @param key Redis key
     */
    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
