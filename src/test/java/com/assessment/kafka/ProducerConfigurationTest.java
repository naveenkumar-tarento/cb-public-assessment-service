package com.assessment.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProducerConfigurationTest {

    private final ProducerConfiguration configuration = new ProducerConfiguration();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(configuration, "kafkabootstrapAddress", "localhost:9092");
    }

    @Test
    void producerFactoryShouldNotBeNull() {
        ProducerFactory<String, String> factory = configuration.producerFactory();
        assertNotNull(factory);
    }

    @Test
    void kafkaTemplateShouldNotBeNull() {
        KafkaTemplate<String, String> template = configuration.kafkaTemplate();
        assertNotNull(template);
    }
}
