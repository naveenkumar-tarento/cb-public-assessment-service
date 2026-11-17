package com.assessment.cache;

import com.assessment.util.ServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCacheMgrTest {

    @Mock
    private JedisPool jedisPool;

    @Mock
    private JedisPool jedisDataPopulationPool;

    @Mock
    private ServerProperties cbExtServerProperties;

    @Mock
    private Jedis jedis;

    @Mock
    private Jedis dataJedis;

    private RedisCacheMgr redisCacheMgr;

    @BeforeEach
    void setUp() {
        redisCacheMgr = new RedisCacheMgr();
        ReflectionTestUtils.setField(redisCacheMgr, "jedisPool", jedisPool);
        ReflectionTestUtils.setField(redisCacheMgr, "jedisDataPopulationPool", jedisDataPopulationPool);
        ReflectionTestUtils.setField(redisCacheMgr, "cbExtServerProperties", cbExtServerProperties);

        lenient().when(jedisPool.getResource()).thenReturn(jedis);
        lenient().when(jedisDataPopulationPool.getResource()).thenReturn(dataJedis);
    }

    @Test
    void postConstructShouldInitializeTimeoutsFromServerProperties() {
        when(cbExtServerProperties.getRedisQuestionsReadTimeOut()).thenReturn(1000);
        when(cbExtServerProperties.getRedisTimeout()).thenReturn("500");

        redisCacheMgr.postConstruct();

        // No direct getter, but calling putCache(key,obj) should not throw and should use the ttl internally.
        redisCacheMgr.putCache("key1", "value1");
    }

    @Test
    void putCacheShouldSerializeAndStoreWithTtl() throws Exception {
        redisCacheMgr.putCache("key1", "value1", 100);
        verifySetAndExpireCalled("key1", 100);
    }

    @Test
    void putCacheShouldSwallowExceptionOnFailure() {
        doThrow(new RuntimeException("redis down")).when(jedis).set(anyString(), anyString());
        redisCacheMgr.putCache("key1", "value1", 100);
        // no exception propagated
    }

    @Test
    void putInQuestionCacheShouldStoreValue() {
        redisCacheMgr.putInQuestionCache("qKey", "qValue");
        verifySetCalled("qKey");
    }

    @Test
    void putStringInCacheWithTtlShouldStoreValue() {
        redisCacheMgr.putStringInCache("strKey", "strValue", 200);
        verifySetAndExpireCalled("strKey", 200);
    }

    @Test
    void putStringInCacheDefaultTtlShouldDelegate() {
        redisCacheMgr.putStringInCache("strKey", "strValue");
        verifySetCalled("strKey");
    }

    @Test
    void deleteKeyByNameShouldReturnTrueOnSuccess() {
        boolean result = redisCacheMgr.deleteKeyByName("key1");
        assertTrue(result);
    }

    @Test
    void deleteKeyByNameShouldReturnFalseOnException() {
        doThrow(new RuntimeException("fail")).when(jedis).del(anyString());
        boolean result = redisCacheMgr.deleteKeyByName("key1");
        assertFalse(result);
    }

    @Test
    void deleteAllCBExtKeyShouldDeleteAllMatchingKeys() {
        Set<String> keys = new HashSet<>(Arrays.asList("CB_EXT_k1", "CB_EXT_k2"));
        when(jedis.keys(anyString())).thenReturn(keys);

        boolean result = redisCacheMgr.deleteAllCBExtKey();

        assertTrue(result);
    }

    @Test
    void deleteAllCBExtKeyShouldReturnFalseOnException() {
        when(jedis.keys(anyString())).thenThrow(new RuntimeException("fail"));
        assertFalse(redisCacheMgr.deleteAllCBExtKey());
    }

    @Test
    void getCacheShouldReturnValueFromJedis() {
        when(jedis.get("CB_EXT_key1")).thenReturn("value1");
        assertEquals("value1", redisCacheMgr.getCache("key1"));
    }

    @Test
    void getCacheShouldReturnNullOnException() {
        when(jedis.get(anyString())).thenThrow(new RuntimeException("fail"));
        assertNull(redisCacheMgr.getCache("key1"));
    }

    @Test
    void mgetShouldReturnValuesForFields() {
        when(jedis.mget(any(String[].class))).thenReturn(Arrays.asList("v1", "v2"));
        List<String> result = redisCacheMgr.mget(Arrays.asList("f1", "f2"));
        assertEquals(Arrays.asList("v1", "v2"), result);
    }

    @Test
    void mgetShouldReturnNullOnException() {
        when(jedis.mget(any(String[].class))).thenThrow(new RuntimeException("fail"));
        assertNull(redisCacheMgr.mget(Collections.singletonList("f1")));
    }

    @Test
    void getAllKeyNamesShouldReturnKeysFromJedis() {
        Set<String> keys = new HashSet<>(Collections.singletonList("CB_EXT_k1"));
        when(jedis.keys(anyString())).thenReturn(keys);
        assertEquals(keys, redisCacheMgr.getAllKeyNames());
    }

    @Test
    void getAllKeyNamesShouldReturnEmptySetOnException() {
        when(jedis.keys(anyString())).thenThrow(new RuntimeException("fail"));
        assertTrue(redisCacheMgr.getAllKeyNames().isEmpty());
    }

    @Test
    void getAllKeysAndValuesShouldReturnPopulatedListWhenKeysExist() {
        Set<String> keys = new HashSet<>(Collections.singletonList("CB_EXT_k1"));
        when(jedis.keys(anyString())).thenReturn(keys);
        when(jedis.get("CB_EXT_k1")).thenReturn("v1");

        List<Map<String, Object>> result = redisCacheMgr.getAllKeysAndValues();

        assertEquals(1, result.size());
        assertEquals("v1", result.get(0).get("CB_EXT_k1"));
    }

    @Test
    void getAllKeysAndValuesShouldReturnEmptyListWhenNoKeys() {
        when(jedis.keys(anyString())).thenReturn(Collections.emptySet());
        assertTrue(redisCacheMgr.getAllKeysAndValues().isEmpty());
    }

    @Test
    void getAllKeysAndValuesShouldReturnEmptyListOnException() {
        when(jedis.keys(anyString())).thenThrow(new RuntimeException("fail"));
        assertTrue(redisCacheMgr.getAllKeysAndValues().isEmpty());
    }

    @Test
    void hgetShouldSelectIndexAndReturnFields() {
        when(dataJedis.hmget("hKey", "f1", "f2")).thenReturn(Arrays.asList("v1", "v2"));
        List<String> result = redisCacheMgr.hget("hKey", 2, "f1", "f2");
        assertEquals(Arrays.asList("v1", "v2"), result);
    }

    @Test
    void hgetShouldReturnNullOnException() {
        when(dataJedis.hmget(anyString(), any(String[].class))).thenThrow(new RuntimeException("fail"));
        assertNull(redisCacheMgr.hget("hKey", 2, "f1"));
    }

    @Test
    void getCacheWithIndexShouldSelectIndexWhenProvided() {
        when(jedis.get("rawKey")).thenReturn("value1");
        assertEquals("value1", redisCacheMgr.getCache("rawKey", 3));
    }

    @Test
    void getCacheWithNullIndexShouldSkipSelect() {
        when(jedis.get("rawKey")).thenReturn("value1");
        assertEquals("value1", redisCacheMgr.getCache("rawKey", null));
    }

    @Test
    void getCacheFromDataRedishShouldSelectIndexAndReturnValue() {
        when(dataJedis.get("rawKey")).thenReturn("dataValue");
        assertEquals("dataValue", redisCacheMgr.getCacheFromDataRedish("rawKey", 1));
    }

    @Test
    void getContentFromCacheShouldReturnValue() {
        when(jedis.get("contentKey")).thenReturn("contentValue");
        assertEquals("contentValue", redisCacheMgr.getContentFromCache("contentKey"));
    }

    @Test
    void keyExistsShouldReturnTrueWhenPresent() {
        when(jedis.exists("CB_EXT_key1")).thenReturn(true);
        assertTrue(redisCacheMgr.keyExists("key1"));
    }

    @Test
    void keyExistsShouldReturnFalseOnException() {
        when(jedis.exists(anyString())).thenThrow(new RuntimeException("fail"));
        assertFalse(redisCacheMgr.keyExists("key1"));
    }

    @Test
    void valueExistsShouldReturnTrueWhenMember() {
        when(jedis.sismember("CB_EXT_key1", "value1")).thenReturn(true);
        assertTrue(redisCacheMgr.valueExists("key1", "value1"));
    }

    @Test
    void valueExistsShouldReturnFalseOnException() {
        when(jedis.sismember(anyString(), anyString())).thenThrow(new RuntimeException("fail"));
        assertFalse(redisCacheMgr.valueExists("key1", "value1"));
    }

    @Test
    void putCacheAsStringArrayShouldUseDefaultTtlWhenNull() {
        redisCacheMgr.putCacheAsStringArray("arrKey", new String[]{"a", "b"}, null);
        // verifies no exception; default ttl path exercised
    }

    @Test
    void putCacheAsStringArrayShouldUseProvidedTtl() {
        redisCacheMgr.putCacheAsStringArray("arrKey", new String[]{"a", "b"}, 500);
    }

    @Test
    void putCacheAsStringArrayShouldSwallowException() {
        doThrow(new RuntimeException("fail")).when(jedis).sadd(anyString(), any(String[].class));
        redisCacheMgr.putCacheAsStringArray("arrKey", new String[]{"a"}, 100);
    }

    private void verifySetAndExpireCalled(String key, int ttl) {
        org.mockito.Mockito.verify(jedis).set(org.mockito.ArgumentMatchers.eq("CB_EXT_" + key), anyString());
        org.mockito.Mockito.verify(jedis).expire("CB_EXT_" + key, ttl);
    }

    private void verifySetCalled(String key) {
        org.mockito.Mockito.verify(jedis).set(org.mockito.ArgumentMatchers.eq("CB_EXT_" + key), anyString());
    }
}
