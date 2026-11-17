package com.assessment.util.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectCommonExceptionTest {

    @Test
    void constructorShouldSetAllFields() {
        ProjectCommonException exception = new ProjectCommonException("CODE1", "some message", 400);

        assertEquals("CODE1", exception.getCode());
        assertEquals("some message", exception.getMessage());
        assertEquals(400, exception.getResponseCode());
    }

    @Test
    void shouldAllowSettingFieldsAfterConstruction() {
        ProjectCommonException exception = new ProjectCommonException("CODE1", "msg", 400);

        exception.setCode("CODE2");
        exception.setMessage("new message");
        exception.setResponseCode(500);

        assertEquals("CODE2", exception.getCode());
        assertEquals("new message", exception.getMessage());
        assertEquals(500, exception.getResponseCode());
    }
}
