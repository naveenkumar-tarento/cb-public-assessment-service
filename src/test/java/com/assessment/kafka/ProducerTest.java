package com.assessment.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void pushShouldSerializeAndSendMessage() {
        Producer producer = new Producer();
        producer.kafkaTemplate = kafkaTemplate;

        Map<String, Object> value = new HashMap<>();
        value.put("key", "value");

        producer.push("topic1", value);

        verify(kafkaTemplate).send(anyString(), anyString());
    }

    @Test
    void pushWithKeyShouldSerializeAndSendMessage() {
        Producer producer = new Producer();
        producer.kafkaTemplate = kafkaTemplate;

        Map<String, Object> value = new HashMap<>();
        value.put("key", "value");

        producer.pushWithKey("topic1", value, "msgKey");

        verify(kafkaTemplate).send(anyString(), anyString(), anyString());
    }
}
