package com.assessment.consumer;

import com.assessment.service.AssessmentServiceV5;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.MissingFormatArgumentException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private AssessmentServiceV5 assessmentServiceV5;

    private final NotificationConsumer consumer = new NotificationConsumer();

    @Test
    void notificationConsumerShouldInvokeProcessNotification() {
        ReflectionTestUtils.setField(consumer, "assessmentServiceV5", assessmentServiceV5);

        ConsumerRecord<String, String> consumerRecord =
                new ConsumerRecord<>("topic", 0, 0, "key", "{\"userId\":\"u1\"}");
        consumer.notificationConsumer(consumerRecord);

        verify(assessmentServiceV5, timeout(2000)).processNotification(anyMap());
    }

    @Test
    void notifyDownloadCertificateShouldInvokeProcessDownloadNotification() {
        ReflectionTestUtils.setField(consumer, "assessmentServiceV5", assessmentServiceV5);

        ConsumerRecord<String, String> consumerRecord =
                new ConsumerRecord<>("topic", 0, 0, "key", "{\"userId\":\"u1\"}");
        consumer.notifyDownloadCertificate(consumerRecord);

        verify(assessmentServiceV5, timeout(2000)).processDownloadNotification(anyMap());
    }

    @Test
    void notificationConsumerShouldThrowOnInvalidJsonDueToMalformedErrorLogFormat() {
        // Current behaviour: the catch block builds its format string as
        // "...%s " + e.getMessage() and then calls String.format on it with no
        // varargs, so the literal "%s" throws MissingFormatArgumentException
        // instead of the original parse error being swallowed.
        ReflectionTestUtils.setField(consumer, "assessmentServiceV5", assessmentServiceV5);

        ConsumerRecord<String, String> consumerRecord =
                new ConsumerRecord<>("topic", 0, 0, "key", "not-valid-json");

        assertThrows(MissingFormatArgumentException.class,
                () -> consumer.notificationConsumer(consumerRecord));
    }
}
