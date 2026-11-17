package com.assessment.keys;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeoLocationJsonKeyTest {

    @Test
    void shouldExposeExpectedConstants() throws Exception {
        assertEquals("parentCode", GeoLocationJsonKey.PARENT_CODE);
        assertEquals("id", GeoLocationJsonKey.ID);

        Constructor<GeoLocationJsonKey> constructor = GeoLocationJsonKey.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
