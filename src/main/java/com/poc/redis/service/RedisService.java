package com.poc.redis.service;

import io.lettuce.core.RedisConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis Service using single RedisTemplate
 * The single template automatically routes:
 * - Reads to replicas (when available) or master (as fallback)
 * - Writes to master
 */
@Service
@Slf4j
public class RedisService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private LettuceConnectionFactory redisConnectionFactory;
    
    /**
     * Write operations - automatically routed to master
     */
    public void setValue(String key, Object value) {
        log.info("Setting value: {} = {}", key, value);
        redisTemplate.opsForValue().set(key, value);
        log.info("Value set successfully to master");
    }

    /**
     * Read operations - automatically prefer replica, fallback to master
     */
    public Object getValue(String key) {
        log.info("Getting value for key: {}", key);
        Object value = redisTemplate.opsForValue().get(key);
        log.info("Retrieved value: {} = {}", key, value);
        return value;
    }

    /**
     * Delete operations - automatically routed to master
     */
    public Boolean deleteKey(String key) {
        log.info("Deleting key: {}", key);
        Boolean result = redisTemplate.delete(key);
        log.info("Delete operation result for {}: {}", key, result);
        return result;
    }

    /**
     * Check if key exists - can be read from replica
     */
    public Boolean hasKey(String key) {
        log.info("Checking if key exists: {}", key);
        Boolean exists = redisTemplate.hasKey(key);
        log.info("Key {} exists: {}", key, exists);
        return exists;
    }

    /**
     * Set with expiration - routed to master
     */
    public void setValueWithTTL(String key, Object value, long seconds) {
        log.info("Setting value with TTL: {} = {} (expires in {} seconds)", key, value, seconds);
        redisTemplate.opsForValue().set(key, value, java.time.Duration.ofSeconds(seconds));
        log.info("Value with TTL set successfully");
    }

    /**
     * Increment operation - routed to master
     */
    public Long increment(String key) {
        log.info("Incrementing key: {}", key);
        Long result = redisTemplate.opsForValue().increment(key);
        log.info("Increment result for {}: {}", key, result);
        return result;
    }

    /**
     * Health check for Redis connection
     */
    public Health checkRedisHealth() {
        try {
            // Simple ping test
            redisTemplate.opsForValue().get("health-check");
            log.info("Redis health check passed");
            return Health.up()
                    .withDetail("status", "Connected")
                    .withDetail("connectionFactory", redisConnectionFactory.getClass().getSimpleName())
                    .build();
        } catch (RedisConnectionException e) {
            log.error("Redis health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("status", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Redis health check error: {}", e.getMessage());
            return Health.down()
                    .withDetail("status", "Error")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * Get connection info for debugging
     */
    public String getConnectionInfo() {
        return String.format("Connection Factory: %s, Template: %s", 
                redisConnectionFactory.getClass().getSimpleName(),
                redisTemplate.getClass().getSimpleName());
    }
}