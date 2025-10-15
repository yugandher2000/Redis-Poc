package com.poc.redis.config;


import com.poc.redis.redisCacheManagement.FallbackRedisCacheManager;
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
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
@EnableCaching
public class RedisConfig {

    /** we need a template and the connection factory to create a cache manager
     if we have another redis instance for example a replica, we can create another template and connection factory and use it to create another cache manager
     */
    @Autowired
    ApplicationProperties applicationProperties;
    @Bean(name = "redisTemplate")  //single master Template
    public RedisTemplate<String,Object> redisTemplate(@Qualifier("masterConnectionFactory") RedisConnectionFactory factory){
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
    @Bean(name = "masterConnectionFactory") //single master connection factory we are using lettuce
    public LettuceConnectionFactory connectionFactory(){
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(applicationProperties.getRedis().getHost());
        config.setPort(applicationProperties.getRedis().getPort());
        // if we have password we can set it here
        log.info("Redis Connection Factory - HOST: {}, PORT: {}", config.getHostName(), config.getPort());
        return new LettuceConnectionFactory(config);
    }
    @Bean(name = "masterCacheManager") //single master cache manager
    public RedisCacheManager masterCacheManager(@Qualifier("redisTemplate") RedisTemplate<String, Object> redisTemplate){
        log.info("using {} as a connection factory for cache manager", redisTemplate.getConnectionFactory());
        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisTemplate.getConnectionFactory());
        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
                .build();
    }

    @Primary
    @Bean
    public CacheManager cacheManager(@Qualifier("masterCacheManager") RedisCacheManager masterCacheManager){
        return new FallbackRedisCacheManager(masterCacheManager);
    }
}
