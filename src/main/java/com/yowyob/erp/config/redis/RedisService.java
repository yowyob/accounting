package com.yowyob.erp.config.redis;

import java.time.Duration;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Reactive Redis utility service for:
 * - session management
 * - application cache
 * - account balances
 * - key verification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final ReactiveRedisTemplate<String, Object> reactive_redis_template;
    private final ObjectMapper object_mapper;

    /**
     * Saves a value with TTL
     */
    public Mono<Boolean> save(String key, Object value, Duration ttl) {
        return reactive_redis_template.opsForValue().set(key, value, ttl)
                .doOnSuccess(v -> log.debug("💾 Value saved in Redis: {}", key))
                .doOnError(e -> log.error("❌ Error saving to Redis for key {}", key, e))
                .onErrorReturn(false);
    }

    /**
     * Retrieves a value with expected type
     */
    public <T> Mono<T> get(String key, Class<T> type) {
        return reactive_redis_template.opsForValue().get(key)
                .map(value -> object_mapper.convertValue(value, type))
                .doOnError(e -> log.error("❌ Error retrieving from Redis for key {}", key, e))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * Deletes a key
     */
    public Mono<Long> delete(String key) {
        return reactive_redis_template.delete(key)
                .doOnSuccess(v -> log.debug("🗑️ Key deleted: {}", key))
                .doOnError(e -> log.error("❌ Error deleting from Redis for key {}", key, e))
                .onErrorReturn(0L);
    }

    /**
     * Checks if a key exists
     */
    public Mono<Boolean> exists(String key) {
        return reactive_redis_template.hasKey(key)
                .doOnError(e -> log.error("❌ Error checking Redis existence for key: {}", key, e))
                .onErrorReturn(false);
    }

    /**
     * Account balance management
     */
    public Mono<Boolean> saveAccountBalance(String organization_id, String account_number, Double balance) {
        return save(String.format("account:balance:%s:%s", organization_id, account_number), balance, Duration.ofMinutes(30));
    }

    public Mono<Double> getAccountBalance(String organization_id, String account_number) {
        return get(String.format("account:balance:%s:%s", organization_id, account_number), Double.class);
    }

    /**
     * User session management
     */
    public Mono<Boolean> saveUserSession(String session_id, Object user_info, Duration ttl) {
        return save("session:" + session_id, user_info, ttl);
    }

    public <T> Mono<T> getUserSession(String session_id, Class<T> type) {
        return get("session:" + session_id, type);
    }
}
