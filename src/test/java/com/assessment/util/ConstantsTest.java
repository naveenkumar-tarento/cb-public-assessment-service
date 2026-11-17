package com.assessment.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstantsTest {

    @Test
    void shouldExposeExpectedConstantValues() {
        assertEquals("email", Constants.EMAIL);
        assertEquals("userId", Constants.USER_ID);
        assertEquals("success", Constants.SUCCESS);
    }

    @Test
    void constructorShouldThrowIllegalStateException() throws NoSuchMethodException {
        Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertTrue(ex.getCause() instanceof IllegalStateException);
    }
}
