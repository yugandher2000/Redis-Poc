package com.poc.redis.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Configuration with Single Template
 * This configuration uses one RedisTemplate and ConnectionFactory that can handle both reads and writes
 * by leveraging Lettuce's ReadFrom strategy.
 */
@Configuration
@Slf4j
@EnableCaching
public class RedisConfig {

    @Autowired
    ApplicationProperties applicationProperties;

    /**
     * Single RedisTemplate that handles both reads and writes
     * - Writes automatically go to master
     * - Reads can be configured to prefer replicas (ReadFrom.REPLICA_PREFERRED)
     */
    @Bean(name = "redisTemplate")
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        
        // Configure serializers
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        // Enable transactions (writes will go to master)
        redisTemplate.setEnableTransactionSupport(true);
        redisTemplate.afterPropertiesSet();
        
        log.info("Single RedisTemplate configured successfully with connection factory: {}", 
                connectionFactory.getClass().getSimpleName());
        return redisTemplate;
    }

    /**
     * Single Connection Factory with intelligent read/write routing
     * - Uses REPLICA_PREFERRED strategy: reads from replicas when available, fallback to master
     * - Writes always go to master automatically
     */
    @Bean(name = "redisConnectionFactory")
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        if (applicationProperties.getRedis().getSentinel().isEnabled()) {
            log.info("Creating SENTINEL Connection Factory with REPLICA_PREFERRED strategy");
            return createSentinelConnectionFactory();
        } else {
            log.info("Creating STANDALONE Connection Factory");
            return createStandaloneConnectionFactory();
        }
    }

    /**
     * Cache Manager using the single connection factory
     */
    @Bean(name = "cacheManager")
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "app-cache:" + cacheName + "::")
                .entryTtl(applicationProperties.getRedis().getTtl());

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }

    // Helper method to create Sentinel connection factory
    private LettuceConnectionFactory createSentinelConnectionFactory() {
        // Create Sentinel configuration
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
        sentinelConfig.setMaster(applicationProperties.getRedis().getSentinel().getMaster()); //myMaster
        
        // Add sentinel nodes
        for (String node : applicationProperties.getRedis().getSentinel().getNodes()) {
            String[] parts = node.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            sentinelConfig.addSentinel(new RedisNode(host, port));
            log.info("Added Sentinel node: {}:{}", host, port);
        }
        
        // Set passwords if provided
        if (applicationProperties.getRedis().getPassword() != null && 
            !applicationProperties.getRedis().getPassword().isEmpty()) {
            sentinelConfig.setPassword(applicationProperties.getRedis().getPassword());
        }
        if (applicationProperties.getRedis().getSentinel().getPassword() != null && 
            !applicationProperties.getRedis().getSentinel().getPassword().isEmpty()) {
            sentinelConfig.setSentinelPassword(applicationProperties.getRedis().getSentinel().getPassword());
        }
        
        // Set database
        sentinelConfig.setDatabase(applicationProperties.getRedis().getSentinel().getDatabase());
        
        // Configure Lettuce client with REPLICA_PREFERRED strategy
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .readFrom(ReadFrom.REPLICA_PREFERRED) // Key setting: reads prefer replicas, writes go to master
                .clientOptions(ClientOptions.builder()
                        .autoReconnect(true)
                        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                        .build())
                .commandTimeout(Duration.ofSeconds(5))
                .build();
        // This configuration enables intelligent read/write splitting with automatic failover capabilities
        
        log.info("Sentinel Configuration - Master: {}, ReadFrom: REPLICA_PREFERRED", 
                applicationProperties.getRedis().getSentinel().getMaster());
        
        return new LettuceConnectionFactory(sentinelConfig, clientConfig);
    }

    // Helper method to create Standalone connection factory
    private LettuceConnectionFactory createStandaloneConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(applicationProperties.getRedis().getHost());
        config.setPort(applicationProperties.getRedis().getPort());
        
        if (applicationProperties.getRedis().getPassword() != null && 
            !applicationProperties.getRedis().getPassword().isEmpty()) {
            config.setPassword(applicationProperties.getRedis().getPassword());
        }
        
        log.info("Standalone Redis Configuration - HOST: {}, PORT: {}", 
                config.getHostName(), config.getPort());
        return new LettuceConnectionFactory(config);
    }
}