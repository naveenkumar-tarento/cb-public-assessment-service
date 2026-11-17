package com.assessment.cache;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DataCacheMgrTest {

    private final DataCacheMgr cacheMgr = new DataCacheMgr();

    @Test
    void shouldStoreAndRetrieveString() {
        cacheMgr.putStringInCache("key1", "value1");
        assertEquals("value1", cacheMgr.getStringFromCache("key1"));
    }

    @Test
    void shouldReturnEmptyStringWhenKeyMissing() {
        assertEquals("", cacheMgr.getStringFromCache("missing"));
    }

    @Test
    void shouldStoreAndRetrieveObject() {
        cacheMgr.putObjectInCache("objKey", 42);
        assertEquals(42, cacheMgr.getObjectFromCache("objKey"));
    }

    @Test
    void shouldReturnNullWhenObjectKeyMissing() {
        assertNull(cacheMgr.getObjectFromCache("missing"));
    }

    @Test
    void shouldStoreAndRetrieveContent() {
        Map<String, Object> content = Collections.singletonMap("field", "value");
        cacheMgr.putContentInCache("contentKey", content);
        assertEquals(content, cacheMgr.getContentFromCache("contentKey"));
    }

    @Test
    void shouldReturnNullWhenContentKeyMissing() {
        assertNull(cacheMgr.getContentFromCache("missing"));
    }
}
