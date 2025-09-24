package com.poc.redis.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RedisIntegrationTest {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void testRedisSetAndGet() {
        redisTemplate.opsForValue().set("email", "ybalasaraswa@opentext.com");
        String email = redisTemplate.opsForValue().get("email");
        assertEquals("ybalasaraswa@opentext.com", email);
        System.out.println("Cached Value from Redis: " + email);
    }
}

