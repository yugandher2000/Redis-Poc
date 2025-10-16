package com.poc.redis.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "spring.data")
@Data
public class ApplicationProperties {
    private Redis redis;


    @Data
    public static class Redis {
        private String host;
        private int port;
        private Duration ttl;
    }
}
