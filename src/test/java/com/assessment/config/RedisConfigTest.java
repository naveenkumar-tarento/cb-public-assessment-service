package com.assessment.config;

import com.assessment.util.ServerProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.JedisPool;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.lenient;

import java.time.Duration;

@ExtendWith(MockitoExtension.class)
class RedisConfigTest {

    @Mock
    private ServerProperties cbProperties;

    @Test
    void jedisPoolShouldBeCreatedFromConfiguredHostAndPort() {
        lenient().when(cbProperties.getRedisHostName()).thenReturn("localhost");
        lenient().when(cbProperties.getRedisPort()).thenReturn("1");
        lenient().when(cbProperties.getRedisDataHostName()).thenReturn("localhost");
        lenient().when(cbProperties.getRedisDataPort()).thenReturn("1");

        RedisConfig redisConfig = new RedisConfig();
        redisConfig.cbProperties = cbProperties;

        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            JedisPool pool = redisConfig.jedisPool();
            assertNotNull(pool);
            pool.close();
        });
    }

    @Test
    void jedisDataPopulationPoolShouldBeCreatedFromConfiguredHostAndPort() {
        lenient().when(cbProperties.getRedisHostName()).thenReturn("localhost");
        lenient().when(cbProperties.getRedisPort()).thenReturn("1");
        lenient().when(cbProperties.getRedisDataHostName()).thenReturn("localhost");
        lenient().when(cbProperties.getRedisDataPort()).thenReturn("1");

        RedisConfig redisConfig = new RedisConfig();
        redisConfig.cbProperties = cbProperties;

        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            JedisPool pool = redisConfig.jedisDataPopulationPool();
            assertNotNull(pool);
            pool.close();
        });
    }
}
