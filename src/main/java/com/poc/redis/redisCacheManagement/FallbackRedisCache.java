package com.poc.redis.redisCacheManagement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

@Slf4j
public class FallbackRedisCache implements Cache {
    private final Cache masterCache;
    private final Cache replicaCache;

    public FallbackRedisCache(Cache masterCache, Cache replicaCache) {
        this.masterCache = masterCache;
        this.replicaCache = replicaCache;
    }
    
    @Override
    public String getName() {
        return masterCache.getName();
    }

    @Override
    public Object getNativeCache() {
        return masterCache.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        // Try reading from replica first (load balancing)
        try {
            ValueWrapper value = replicaCache.get(key);
            if (value != null) {
                log.debug("Cache HIT from replica for key: {}", key);
                return value;
            }
            log.debug("Cache MISS from replica for key: {}, trying master", key);
        } catch (Exception e) {
            log.warn("Failed to read from replica cache, falling back to master: {}", e.getMessage());
        }
        
        // Fallback to master
        try {
            ValueWrapper value = masterCache.get(key);
            if (value != null) {
                log.debug("Cache HIT from master for key: {}", key);
            } else {
                log.debug("Cache MISS from master for key: {}", key);
            }
            return value;
        } catch (Exception e) {
            log.error("Failed to read from both replica and master cache for key: {}", key, e);
            return null;
        }
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        return wrapper != null ? type.cast(wrapper.get()) : null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        try {
            ValueWrapper value = get(key);
            if (value != null) {
                return (T) value.get();
            }

            // Value not found in cache, load it
            T loadedValue = valueLoader.call();
            put(key, loadedValue);
            return loadedValue;

        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        // Always write to master first (source of truth)
        try {
            masterCache.put(key, value);
            log.debug("Successfully wrote to master cache for key: {}", key);
        } catch (Exception e) {
            log.error("Failed to write to master cache for key: {}", key, e);
            // Don't throw exception, let application continue
        }
        
        // Try to write to replica as well for immediate read consistency
        try {
            replicaCache.put(key, value);
            log.debug("Successfully wrote to replica cache for key: {}", key);
        } catch (Exception e) {
            log.warn("Failed to write to replica cache for key: {} (this is ok, will sync later): {}", key, e.getMessage());
            // Don't throw exception - replica writes are best effort
        }
    }

    @Override
    public void evict(Object key) {
        // Evict from master first
        try {
            masterCache.evict(key);
            log.debug("Successfully evicted from master cache for key: {}", key);
        } catch (Exception e) {
            log.error("Failed to evict from master cache for key: {}", key, e);
        }
        
        // Evict from replica as well
        try {
            replicaCache.evict(key);
            log.debug("Successfully evicted from replica cache for key: {}", key);
        } catch (Exception e) {
            log.warn("Failed to evict from replica cache for key: {} (this is ok): {}", key, e.getMessage());
        }
    }

    @Override
    public void clear() {
        // Clear master first
        try {
            masterCache.clear();
            log.debug("Successfully cleared master cache");
        } catch (Exception e) {
            log.error("Failed to clear master cache", e);
        }
        
        // Clear replica as well
        try {
            replicaCache.clear();
            log.debug("Successfully cleared replica cache");
        } catch (Exception e) {
            log.warn("Failed to clear replica cache (this is ok): {}", e.getMessage());
        }
    }
}
