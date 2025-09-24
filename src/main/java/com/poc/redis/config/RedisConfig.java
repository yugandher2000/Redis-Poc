package com.poc.redis.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * Creates and configures a RedisTemplate bean for interacting with Redis.
     * RedisTemplate provides high-level operations and serialization control.
     */
    @Bean(name = "redisTemplate")
    public RedisTemplate redisTemplate(RedisConnectionFactory factory){
        // RedisConnectionFactory: Sets up the connection to the Redis server and manages connections.
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory); // Connects RedisTemplate to Redis using the provided factory.
        redisTemplate.setKeySerializer(new StringRedisSerializer()); // Serializes keys as readable UTF-8 strings.
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer()); // Serializes values using Java's built-in serialization for complex objects.
        redisTemplate.setHashKeySerializer(new StringRedisSerializer()); // Serializes hash keys as strings for readability.
        redisTemplate.setEnableTransactionSupport(true); // Enables transaction support for atomic operations in Redis.
        redisTemplate.afterPropertiesSet(); // Finalizes the template setup before use.
        return redisTemplate;
    }

}
