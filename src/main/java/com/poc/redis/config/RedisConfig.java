package com.poc.redis.config;


import com.poc.redis.redisCacheManagement.FallbackRedisCacheManager;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
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

@Configuration
@Slf4j
@EnableCaching
public class RedisConfig {

    @Autowired
    ApplicationProperties applicationProperties;

    @Bean(name = "redisTemplate")  //Master Template for writes
    public RedisTemplate<String,Object> redisTemplate(@Qualifier("masterConnectionFactory") RedisConnectionFactory factory){
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setEnableTransactionSupport(true);
        redisTemplate.afterPropertiesSet();
        log.info("Master RedisTemplate configured successfully");
        return redisTemplate;
    }

    @Bean(name = "replicaRedisTemplate")  //Replica Template for reads
    public RedisTemplate<String,Object> replicaTemplate(@Qualifier("replicaConnectionFactory") RedisConnectionFactory factory){
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setEnableTransactionSupport(false); // Replicas are read-only
        redisTemplate.afterPropertiesSet();
        log.info("Replica RedisTemplate configured successfully");
        return redisTemplate;
    }

    @Primary
    @Bean(name = "masterConnectionFactory") //SENTINEL connection factory for MASTER writes
    public LettuceConnectionFactory connectionFactory(){
        if (applicationProperties.getRedis().getSentinel().isEnabled()) {
            log.info("Creating SENTINEL Master Connection Factory");
            return createSentinelConnectionFactory(ReadFrom.MASTER);
        } else {
            log.info("Creating STANDALONE Master Connection Factory");
            return createStandaloneConnectionFactory();
        }
    }

    @Bean(name = "replicaConnectionFactory") //SENTINEL connection factory for REPLICA reads
    public LettuceConnectionFactory replicaConnectionFactory(){
        if (applicationProperties.getRedis().getSentinel().isEnabled()) {
            log.info("Creating SENTINEL Replica Connection Factory");
            return createSentinelConnectionFactory(ReadFrom.REPLICA_PREFERRED);
        } else {
            log.info("Creating STANDALONE Replica Connection Factory (same as master)");
            return createStandaloneConnectionFactory();
        }
    }

    @Bean(name = "masterCacheManager")
    public RedisCacheManager masterCacheManager(@Qualifier("redisTemplate") RedisTemplate<String, Object> redisTemplate){
        log.info("using {} as a connection factory for cache manager", redisTemplate.getConnectionFactory());
        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisTemplate.getConnectionFactory());
        
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "masterNode-> " + cacheName + "::")
                .entryTtl(Duration.ofMinutes(10));
                
        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(cacheConfig)
                .build();
    }

    @Bean(name = "replicaCacheManager")
    public RedisCacheManager replicaCacheManager(@Qualifier("replicaRedisTemplate") RedisTemplate<String, Object> replicaRedisTemplate){
        log.info("Creating Replica Cache Manager using connection factory: {}", replicaRedisTemplate.getConnectionFactory());
        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(replicaRedisTemplate.getConnectionFactory());
        
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "replicaNode-> " + cacheName + "::")
                .entryTtl(Duration.ofMinutes(10));
                
        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(cacheConfig)
                .build();
    }

    @Primary
    @Bean(name = "cacheManager")
    public CacheManager cacheManager(@Qualifier("masterCacheManager") RedisCacheManager masterCacheManager,
                                   @Qualifier("replicaCacheManager") RedisCacheManager replicaCacheManager){
        return new FallbackRedisCacheManager(masterCacheManager, replicaCacheManager);
    }

    // Helper method to create Sentinel connection factory
    private LettuceConnectionFactory createSentinelConnectionFactory(ReadFrom readFrom) {
        // Create Sentinel configuration
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
        sentinelConfig.setMaster(applicationProperties.getRedis().getSentinel().getMaster());
        
        // Add sentinel nodes
        for (String node : applicationProperties.getRedis().getSentinel().getNodes()) {
            String[] parts = node.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            sentinelConfig.addSentinel(new RedisNode(host, port));
            log.info("Added Sentinel node: {}:{}", host, port);
        }
        
        // Set passwords if provided
        if (applicationProperties.getRedis().getPassword() != null && !applicationProperties.getRedis().getPassword().isEmpty()) {
            sentinelConfig.setPassword(applicationProperties.getRedis().getPassword());
        }
        if (applicationProperties.getRedis().getSentinel().getPassword() != null && !applicationProperties.getRedis().getSentinel().getPassword().isEmpty()) {
            sentinelConfig.setSentinelPassword(applicationProperties.getRedis().getSentinel().getPassword());
        }
        
        // Set database
        sentinelConfig.setDatabase(applicationProperties.getRedis().getSentinel().getDatabase());
        
        // Configure Lettuce client
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .readFrom(readFrom)
                .clientOptions(ClientOptions.builder()
                        .autoReconnect(true)
                        .build())
                .build();
        
        log.info("Sentinel Configuration - Master: {}, ReadFrom: {}", 
                applicationProperties.getRedis().getSentinel().getMaster(), readFrom);
        
        return new LettuceConnectionFactory(sentinelConfig, clientConfig);
    }

    // Helper method to create Standalone connection factory
    private LettuceConnectionFactory createStandaloneConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(applicationProperties.getRedis().getHost());
        config.setPort(applicationProperties.getRedis().getPort());
        
        if (applicationProperties.getRedis().getPassword() != null && !applicationProperties.getRedis().getPassword().isEmpty()) {
            config.setPassword(applicationProperties.getRedis().getPassword());
        }
        
        log.info("Standalone Redis Configuration - HOST: {}, PORT: {}", config.getHostName(), config.getPort());
        return new LettuceConnectionFactory(config);
    }
}
