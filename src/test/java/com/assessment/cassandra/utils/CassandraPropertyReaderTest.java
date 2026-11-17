package com.assessment.cassandra.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CassandraPropertyReaderTest {

    @Test
    void getInstanceShouldReturnSameSingletonInstance() {
        CassandraPropertyReader first = CassandraPropertyReader.getInstance();
        CassandraPropertyReader second = CassandraPropertyReader.getInstance();
        assertNotNull(first);
        assertSame(first, second);
    }

    @Test
    void readPropertyShouldFallBackToKeyWhenNotConfigured() {
        CassandraPropertyReader reader = CassandraPropertyReader.getInstance();
        assertEquals("unconfigured_column_xyz", reader.readProperty("unconfigured_column_xyz"));
    }
}
