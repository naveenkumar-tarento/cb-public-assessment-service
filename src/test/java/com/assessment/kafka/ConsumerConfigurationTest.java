package com.assessment.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConsumerConfigurationTest {

    private final ConsumerConfiguration configuration = new ConsumerConfiguration();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(configuration, "kafkabootstrapAddress", "localhost:9092");
        ReflectionTestUtils.setField(configuration, "kafkaOffsetResetValue", "earliest");
        ReflectionTestUtils.setField(configuration, "kafkaMaxPollInterval", 300000);
        ReflectionTestUtils.setField(configuration, "kafkaMaxPollRecords", 10);
        ReflectionTestUtils.setField(configuration, "kafkaAutoCommitInterval", 1000);
    }

    @Test
    void consumerConfigsShouldContainExpectedValues() {
        Map<String, Object> props = configuration.consumerConfigs();

        assertEquals("localhost:9092", props.get("bootstrap.servers"));
        assertEquals(true, props.get("enable.auto.commit"));
        assertEquals("earliest", props.get("auto.offset.reset"));
        assertEquals(300000, props.get("max.poll.interval.ms"));
        assertEquals(10, props.get("max.poll.records"));
        assertEquals(1000, props.get("auto.commit.interval.ms"));
    }

    @Test
    void consumerFactoryShouldNotBeNull() {
        ConsumerFactory<String, String> factory = configuration.consumerFactory();
        assertNotNull(factory);
    }

    @Test
    void kafkaListenerContainerFactoryShouldNotBeNull() {
        KafkaListenerContainerFactory<?> factory = configuration.kafkaListenerContainerFactory();
        assertNotNull(factory);
    }
}
