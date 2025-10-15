package com.poc.redis.service;

import io.lettuce.core.RedisConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RedisService {
    @Autowired
    @Qualifier("masterConnectionFactory")
    LettuceConnectionFactory lettuceConnectionFactory;
    public Health health() {
        try {
            log.info("Health Check - Redis MASTER healthCheck. HOST: " + lettuceConnectionFactory.getHostName() + ", PORT: " + lettuceConnectionFactory.getPort() + ", PING : " + lettuceConnectionFactory.getConnection().ping());
            return Health.up().withDetail("message", "Redis is up and running").build();

        } catch (RedisConnectionException e) {
            return Health.down().withException(e).withDetail("message", "Redis connection failed").build();
        } catch (Exception e) {
            return Health.down().withException(e).withDetail("message", "Redis health check failed").build();
        }
    }
}
