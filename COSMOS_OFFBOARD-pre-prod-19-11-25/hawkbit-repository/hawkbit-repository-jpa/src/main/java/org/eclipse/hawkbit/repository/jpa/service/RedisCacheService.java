package org.eclipse.hawkbit.repository.jpa.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Service for interacting with Redis cache.
 * Provides methods to put, get, evict, check existence, and get TTL for cache keys.
 */
@Service
@Slf4j
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.redis.enabled:false}")
    private boolean redisEnabled;

    /**
     * Constructs a RedisCacheService with the provided RedisTemplate.
     *
     * @param redisTemplate the RedisTemplate to use for cache operations
     */
    public RedisCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Stores a value in the cache with the given key.
     *
     * @param key   the cache key
     * @param value the value to store
     */
    public void put(String key, Object value) {

        if (!isRedisEnabled()) return;

        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (DataAccessException e) {
            log.error("Failed to put value in Redis cache for key: {}", key, e);
            throw new RuntimeException("Failed to put value in Redis cache for key: " + key, e);
        }
    }

    /**
     * Stores a value in the cache with the given key and TTL.
     *
     * @param key        the cache key
     * @param value      the value to store
     * @param ttlSeconds time-to-live in seconds
     */
    public void put(String key, Object value, long ttlSeconds) {
        if (!isRedisEnabled()) return;

        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } catch (DataAccessException e) {

            throw new RuntimeException("Failed to put value with TTL in Redis cache for key: " + key, e);
        }
    }

    /**
     * Retrieves a value from the cache and casts it to the specified type.
     *
     * @param key   the cache key
     * @param clazz the expected class type
     * @param <T>   the type of the value
     * @return an Optional containing the value if present and of the correct type, otherwise empty
     */
    public <T> Optional<T> get(String key, Class<T> clazz) {
        if (!isRedisEnabled()) return Optional.empty();

        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            if (clazz.isInstance(value)) {
                return Optional.of(clazz.cast(value));
            }
            throw new ClassCastException("Cached value is not of expected type " + clazz.getName());
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to get value from Redis cache for key: " + key, e);
        }
    }

    /**
     * Retrieves the remaining TTL (in seconds) for a given key in the cache.
     * Returns -2 if the key does not exist, -1 if the key exists but has no associated expire.
     *
     * @param key the cache key
     * @return TTL in seconds, -2 if key does not exist, -1 if no expire
     */
    public Long getTTL(String key) {
        if (!isRedisEnabled()) return -2L;

        try {
            return redisTemplate.getExpire(key);
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to get TTL from Redis cache for key: " + key, e);
        }
    }

    /**
     * Removes a key from the cache.
     *
     * @param key the cache key to evict
     */
    public void evict(String key) {
        if (!isRedisEnabled()) return;

        try {
            redisTemplate.delete(key);
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to evict key from Redis cache: " + key, e);
        }
    }

    /**
     * Checks if a key exists in the cache.
     *
     * @param key the cache key
     * @return true if the key exists, false otherwise
     */
    public boolean exists(String key) {
        if (!isRedisEnabled()) return false;

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to check existence of key in Redis cache: " + key, e);
        }
    }

    /**
     * Checks if Redis caching is enabled.
     *
     * @return true if Redis caching is enabled, false otherwise
     */
    private boolean isRedisEnabled() {
        if (!redisEnabled) {
            log.debug("Caching is disabled, skipping cache operations.");
            return false;
        }
        return true;
    }
}