package org.eclipse.hawkbit.repository.jpa.service;

import io.qameta.allure.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the RedisCacheService class.
 * This class tests the methods of RedisCacheService to ensure they behave correctly
 * when interacting with a Redis cache.
 */
class RedisCacheServiceTest {

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private RedisCacheService redisCacheService;

    @BeforeEach
    void setUp() throws Exception {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisCacheService = new RedisCacheService(redisTemplate);
        // Set redisEnabled to true for testing purposes
        java.lang.reflect.Field field = RedisCacheService.class.getDeclaredField("redisEnabled");
        field.setAccessible(true);
        field.set(redisCacheService, true);
    }

    @Test
    @Description("Given a key, when put without TTL, then it should succeed")
    void givenKeyValue_whenPutWithoutTTL_thenSuccess() {
        doNothing().when(valueOperations).set("key", "value");
        assertDoesNotThrow(() -> redisCacheService.put("key", "value"));
        verify(valueOperations).set("key", "value");
    }

    @Test
    @Description("Given a key, when put without TTL, then it should throw DataAccessException")
    void givenKeyValue_whenPutWithoutTTL_thenThrowsDataAccessException() {
        doThrow(new DataAccessException("fail") {
        }).when(valueOperations).set("key", "value");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> redisCacheService.put("key", "value"));
        assertTrue(ex.getMessage().contains("Failed to put value in Redis cache"));
    }

    @Test
    @Description("Given a key and value with TTL, when put with TTL, then it should succeed")
    void givenKeyValueTTL_whenPutWithTTL_thenSuccess() {
        doNothing().when(valueOperations).set("key", "value", Duration.ofSeconds(10));
        assertDoesNotThrow(() -> redisCacheService.put("key", "value", 10));
        verify(valueOperations).set("key", "value", Duration.ofSeconds(10));
    }

    @Test
    @Description("Given a key and value with TTL, when put with TTL, then it should throw DataAccessException")
    void givenKeyValueTTL_whenPutWithTTL_thenThrowsDataAccessException() {
        doThrow(new DataAccessException("fail") {
        }).when(valueOperations).set("key", "value", Duration.ofSeconds(10));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> redisCacheService.put("key", "value", 10));
        assertTrue(ex.getMessage().contains("Failed to put value with TTL in Redis cache"));
    }

    @Test
    @Description("Given a key and correct type, when get, then it should return value")
    void givenKeyAndCorrectType_whenGet_thenReturnsValue() {
        when(valueOperations.get("key")).thenReturn("value");
        Optional<String> result = redisCacheService.get("key", String.class);
        assertTrue(result.isPresent());
        assertEquals("value", result.get());
    }

    @Test
    @Description("Given a key and correct type, when get, then it should throw ClassCastException")
    void givenKeyAndWrongType_whenGet_thenThrowsClassCastException() {
        when(valueOperations.get("key")).thenReturn(123);
        assertThrows(ClassCastException.class, () -> redisCacheService.get("key", String.class));
    }

    @Test
    @Description("Given a key absent in cache, when get, then it should return empty Optional")
    void givenKeyAbsent_whenGet_thenReturnsEmptyOptional() {
        when(valueOperations.get("key")).thenReturn(null);
        Optional<String> result = redisCacheService.get("key", String.class);
        assertFalse(result.isPresent());
    }

    @Test
    @Description("Given a key, when get, then it should throw RuntimeException")
    void givenKey_whenGet_thenThrowsDataAccessException() {
        when(valueOperations.get("key")).thenThrow(new DataAccessException("fail") {
        });
        assertThrows(RuntimeException.class, () -> redisCacheService.get("key", String.class));
    }

    @Test
    @Description("Given a key, when getTTL, then it should return TTL")
    void givenKey_whenGetTTL_thenReturnsTTL() {
        when(redisTemplate.getExpire("key")).thenReturn(42L);
        assertEquals(42L, redisCacheService.getTTL("key"));
    }

    @Test
    @Description("Given a key, when getTTL, then it should return 0 if key does not exist")
    void givenKey_whenGetTTL_thenThrowsDataAccessException() {
        when(redisTemplate.getExpire("key")).thenThrow(new DataAccessException("fail") {
        });
        assertThrows(RuntimeException.class, () -> redisCacheService.getTTL("key"));
    }

    @Test
    @Description("Given a key, when evict, then it should succeed")
    void givenKey_whenEvict_thenSuccess() {
        when(redisTemplate.delete("key")).thenReturn(true);
        assertDoesNotThrow(() -> redisCacheService.evict("key"));
        verify(redisTemplate).delete("key");
    }

    @Test
    @Description("Given a key, when evict, then it should return false if key does not exist")
    void givenKey_whenEvict_thenThrowsDataAccessException() {
        doThrow(new DataAccessException("fail") {
        }).when(redisTemplate).delete("key");
        assertThrows(RuntimeException.class, () -> redisCacheService.evict("key"));
    }

    @Test
    @Description("Given a key, when evict, then it should return false if key does not exist")
    void givenKeyExists_whenExists_thenReturnsTrue() {
        when(redisTemplate.hasKey("key")).thenReturn(true);
        assertTrue(redisCacheService.exists("key"));
    }

    @Test
    @Description("Given a key, when exists, then it should return false if key does not exist")
    void givenKeyDoesNotExist_whenExists_thenReturnsFalse() {
        when(redisTemplate.hasKey("key")).thenReturn(false);
        assertFalse(redisCacheService.exists("key"));
    }

    @Test
    @Description("Given a key, when exists, then it should throw RuntimeException")
    void givenKey_whenExists_thenThrowsDataAccessException() {
        when(redisTemplate.hasKey("key")).thenThrow(new DataAccessException("fail") {
        });
        assertThrows(RuntimeException.class, () -> redisCacheService.exists("key"));
    }
}