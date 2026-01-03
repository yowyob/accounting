package com.yowyob.erp.config.redis;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis utility service for:
 * - session management
 * - application cache
 * - account balances
 * - key verification
 *
 * Compliant with Yowyob charter: security, traceability, maintainability
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Saves a value with TTL
     */
    public void save(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl.toSeconds(), TimeUnit.SECONDS);
            log.debug("💾 Value saved in Redis: {}", key);
        } catch (Exception e) {
            log.error("❌ Error saving to Redis for key {}", key, e);
        }
    }

    /**
     * Retrieves a value with expected type
     */
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null)
                return null;
            return objectMapper.convertValue(value, type);
        } catch (Exception e) {
            log.error("❌ Error retrieving from Redis for key {}", key, e);
            return null;
        }
    }

    /**
     * Deletes a key
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("🗑️ Key deleted: {}", key);
        } catch (Exception e) {
            log.error("❌ Error deleting from Redis for key {}", key, e);
        }
    }

    /**
     * Checks if a key exists
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("❌ Error checking Redis existence for key: {}", key, e);
            return false;
        }
    }

    /**
     * Account balance management
     */
    public void saveAccountBalance(String tenantId, String accountNumber, Double balance) {
        save(String.format("account:balance:%s:%s", tenantId, accountNumber), balance, Duration.ofMinutes(30));
    }

    public Double getAccountBalance(String tenantId, String accountNumber) {
        return get(String.format("account:balance:%s:%s", tenantId, accountNumber), Double.class);
    }

    /**
     * User session management
     */
    public void saveUserSession(String sessionId, Object userInfo, Duration ttl) {
        save("session:" + sessionId, userInfo, ttl);
    }

    public <T> T getUserSession(String sessionId, Class<T> type) {
        return get("session:" + sessionId, type);
    }
}
