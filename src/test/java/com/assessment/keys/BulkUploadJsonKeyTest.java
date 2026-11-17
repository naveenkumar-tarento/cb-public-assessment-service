package com.assessment.keys;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BulkUploadJsonKeyTest {

    @Test
    void shouldExposeExpectedConstants() throws Exception {
        assertEquals("taskCount", BulkUploadJsonKey.TASK_COUNT);
        assertEquals("COMPLETED", BulkUploadJsonKey.COMPLETED);

        Constructor<BulkUploadJsonKey> constructor = BulkUploadJsonKey.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
