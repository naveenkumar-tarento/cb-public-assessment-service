package com.assessment.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CassandraConfigTest {

    @Test
    void shouldGetAndSetContactPointsPortAndKeyspace() {
        SunbirdConfig config = new SunbirdConfig();
        config.setContactPoints("127.0.0.1");
        config.setPort(9042);
        config.setKeyspaceName("sunbird");

        assertEquals("127.0.0.1", config.getContactPoints());
        assertEquals(9042, config.getPort());
        assertEquals("sunbird", config.getKeyspaceName());
    }

    @Test
    void metricsShouldBeDisabledByDefault() {
        SunbirdConfig config = new SunbirdConfig();
        assertFalse(config.getMetricsEnabled());
    }
}
