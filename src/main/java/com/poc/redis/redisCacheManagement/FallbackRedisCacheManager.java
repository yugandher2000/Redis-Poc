package com.poc.redis.redisCacheManagement;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class FallbackRedisCacheManager implements CacheManager {
    private final ConcurrentHashMap<String, Cache> cacheMap = new ConcurrentHashMap<>();
    private final CacheManager masterCacheManager;
    // private final CacheManager replicaCacheManager;
    public FallbackRedisCacheManager(CacheManager masterCacheManager) {
        // Constructor can be expanded to initialize master and replica cache managers
        this.masterCacheManager = masterCacheManager;
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name,cacheName ->{
            Cache masterCache = masterCacheManager.getCache(cacheName);
            // Cache replicaCache = replicaCacheManager.getCache(cacheName);
            return new FallbackRedisCache(masterCache);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return masterCacheManager.getCacheNames();
    }
}
