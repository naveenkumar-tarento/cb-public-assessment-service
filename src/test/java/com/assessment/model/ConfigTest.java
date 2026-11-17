package com.assessment.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigTest {

    @Test
    void shouldGetAndSetAllFields() {
        Config config = new Config();
        config.setSender("sender@example.com");
        config.setTopic("topic1");
        config.setOtp("1234");
        config.setSubject("subject");

        assertEquals("sender@example.com", config.getSender());
        assertEquals("topic1", config.getTopic());
        assertEquals("1234", config.getOtp());
        assertEquals("subject", config.getSubject());
    }
}
