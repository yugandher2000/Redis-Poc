package com.poc.redis.service;

import io.lettuce.core.RedisConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RedisService {
    
    @Autowired
    @Qualifier("masterConnectionFactory")
    LettuceConnectionFactory masterConnectionFactory;
    
    @Autowired
    @Qualifier("replicaConnectionFactory")
    LettuceConnectionFactory replicaConnectionFactory;
    
    @Autowired
    @Qualifier("redisTemplate")
    RedisTemplate<String, Object> masterTemplate;
    
    @Autowired
    @Qualifier("replicaRedisTemplate")
    RedisTemplate<String, Object> replicaTemplate;
    
    // Write operations - use master
    public void setValue(String key, Object value) {
        log.info("Writing to master: {} = {}", key, value);
        masterTemplate.opsForValue().set(key, value);
    }

    // Read operations - use replica (with fallback to master)
    public Object getValue(String key) {
        try {
            log.info("Reading from replica: {}", key);
            return replicaTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Failed to read from replica, falling back to master: {}", e.getMessage());
            return masterTemplate.opsForValue().get(key);
        }
    }

    // Delete operations - use master
    public Boolean deleteKey(String key) {
        log.info("Deleting from master: {}", key);
        return masterTemplate.delete(key);
    }

    // Check if key exists - use replica
    public Boolean hasKey(String key) {
        try {
            log.info("Checking key existence on replica: {}", key);
            return replicaTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("Failed to check key on replica, falling back to master: {}", e.getMessage());
            return masterTemplate.hasKey(key);
        }
    }
    
    public Health health() {
        try {
            log.info("Health Check - Redis MASTER healthCheck. Connection: {}", masterConnectionFactory.getConnection().ping());
            
            // Also check replica health
            try {
                log.info("Health Check - Redis REPLICA healthCheck. Connection: {}", replicaConnectionFactory.getConnection().ping());
                return Health.up()
                    .withDetail("master", "UP")
                    .withDetail("replica", "UP")
                    .withDetail("message", "Redis Sentinel cluster is up and running")
                    .build();
            } catch (Exception replicaException) {
                return Health.up()
                    .withDetail("master", "UP")
                    .withDetail("replica", "DOWN")
                    .withDetail("message", "Redis master is up, replica has issues")
                    .build();
            }

        } catch (RedisConnectionException e) {
            return Health.down().withException(e).withDetail("message", "Redis connection failed").build();
        } catch (Exception e) {
            return Health.down().withException(e).withDetail("message", "Redis health check failed").build();
        }
    }
}
