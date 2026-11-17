package com.assessment.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PropertiesCacheTest {

    @Test
    void getInstanceShouldReturnSameSingletonInstance() {
        PropertiesCache first = PropertiesCache.getInstance();
        PropertiesCache second = PropertiesCache.getInstance();
        assertSame(first, second);
    }

    @Test
    void saveAndGetConfigPropertyShouldRoundTrip() {
        PropertiesCache cache = PropertiesCache.getInstance();
        cache.saveConfigProperty("my.test.key", "my-value");
        assertEquals("my-value", cache.getProperty("my.test.key"));
    }

    @Test
    void getPropertyShouldFallBackToKeyWhenNotFound() {
        PropertiesCache cache = PropertiesCache.getInstance();
        assertEquals("unknown.key.xyz", cache.getProperty("unknown.key.xyz"));
    }

    @Test
    void readPropertyShouldReturnNullWhenNotFound() {
        PropertiesCache cache = PropertiesCache.getInstance();
        assertNull(cache.readProperty("another.unknown.key"));
    }

    @Test
    void readPropertyShouldReturnSavedValue() {
        PropertiesCache cache = PropertiesCache.getInstance();
        cache.saveConfigProperty("read.test.key", "read-value");
        assertEquals("read-value", cache.readProperty("read.test.key"));
    }

    @Test
    void readCustomErrorShouldReplaceSpacesWithUnderscoreBeforeLookup() {
        PropertiesCache cache = PropertiesCache.getInstance();
        cache.saveConfigProperty("custom_error_key", "custom-error-value");
        assertEquals("custom-error-value", cache.readCustomError("custom error key"));
    }
}
