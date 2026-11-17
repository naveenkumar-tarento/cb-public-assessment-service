package com.assessment.cache.service;

import com.assessment.cache.RedisCacheMgr;
import com.assessment.model.SBApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceImplTest {

    @Mock
    private RedisCacheMgr redisCache;

    private RedisCacheServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RedisCacheServiceImpl();
        service.redisCache = redisCache;
    }

    @Test
    void deleteCacheShouldReturnOkWhenKeysDeleted() throws Exception {
        when(redisCache.deleteAllCBExtKey()).thenReturn(true);
        SBApiResponse response = service.deleteCache();
        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void deleteCacheShouldReturnNotFoundWhenNoKeysDeleted() throws Exception {
        when(redisCache.deleteAllCBExtKey()).thenReturn(false);
        SBApiResponse response = service.deleteCache();
        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
    }

    @Test
    void getKeysShouldReturnOkWhenKeysExist() throws Exception {
        Set<String> keys = new HashSet<>();
        keys.add("key1");
        when(redisCache.getAllKeyNames()).thenReturn(keys);

        SBApiResponse response = service.getKeys();
        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void getKeysShouldReturnNotFoundWhenNoKeysExist() throws Exception {
        when(redisCache.getAllKeyNames()).thenReturn(Collections.emptySet());
        SBApiResponse response = service.getKeys();
        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
    }

    @Test
    void getKeysAndValuesShouldReturnOkWhenResultsExist() throws Exception {
        List<Map<String, Object>> result = Collections.singletonList(Collections.singletonMap("key1", "value1"));
        when(redisCache.getAllKeysAndValues()).thenReturn(result);

        SBApiResponse response = service.getKeysAndValues();
        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void getKeysAndValuesShouldReturnNotFoundWhenEmpty() throws Exception {
        when(redisCache.getAllKeysAndValues()).thenReturn(Collections.emptyList());
        SBApiResponse response = service.getKeysAndValues();
        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
    }
}
