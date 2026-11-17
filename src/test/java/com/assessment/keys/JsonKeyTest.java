package com.assessment.keys;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonKeyTest {

    @Test
    void shouldExposeExpectedConstants() {
        assertEquals("Anonymous", JsonKey.ANONYMOUS);
        assertEquals("Unauthorized", JsonKey.UNAUTHORIZED);
        assertTrue(JsonKey.USER_UNAUTH_STATES.contains(JsonKey.ANONYMOUS));
        assertTrue(JsonKey.USER_UNAUTH_STATES.contains(JsonKey.UNAUTHORIZED));
    }

    @Test
    void constructorShouldBePrivate() throws Exception {
        Constructor<JsonKey> constructor = JsonKey.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
