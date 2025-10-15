package com.poc.redis.redisCacheManagement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

@Slf4j
public class FallbackRedisCache implements Cache {
    private final Cache masterCache;

    public FallbackRedisCache(Cache masterCache) {
        this.masterCache = masterCache;
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
        return masterCache.get(key);
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

            // Value not found in either cache, load it
            T loadedValue = valueLoader.call();
            put(key, loadedValue);
            return loadedValue;

        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        try {
            masterCache.put(key, value);
        } catch (Exception e) {
            log.info("put() - Failed to write to master cache: " + e.getMessage());
        }
    }

    @Override
    public void evict(Object key) {
        try {
            masterCache.evict(key);
        } catch (Exception e) {
            log.info("evict() - Failed to evict from master cache: " + e.getMessage());
        }
    }

    @Override
    public void clear() {
        try{
            masterCache.clear();
        } catch (Exception e) {
            log.info("clear() - Failed to clear replica cache: " + e.getMessage());
        }
    }
}
