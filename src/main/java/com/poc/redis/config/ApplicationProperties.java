package com.poc.redis.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "spring.data")
@Data
public class ApplicationProperties {
    private Redis redis;
    
    @Data
    public static class Redis {
        private String host;
        private int port;
        private String password;
        private Duration ttl;
        private Sentinel sentinel = new Sentinel();
        
        @Data
        public static class Sentinel {
            private boolean enabled = false;
            private String master;
            private String password;
            private List<String> nodes;
            private int database;
            private boolean enableReadFromReplica;
        }
    }
}
