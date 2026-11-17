package com.assessment.kafka.impl;

import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class KafkaCertificateProducerServiceImplTest {

    @Mock
    private ServerProperties serverProperties;

    private final KafkaCertificateProducerServiceImpl service = new KafkaCertificateProducerServiceImpl();
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "serverProperties", serverProperties);
    }

    @Test
    void convertToTimestampShouldParseValidDate() {
        Timestamp timestamp = KafkaCertificateProducerServiceImpl.convertToTimestamp("01/15/2024");
        assertNotNull(timestamp);
    }

    @Test
    void convertToTimestampShouldReturnNullForInvalidDate() {
        Timestamp timestamp = KafkaCertificateProducerServiceImpl.convertToTimestamp("not-a-date");
        assertNull(timestamp);
    }

    @Test
    void replacePlaceholdersShouldReplaceSimplePlaceholder() throws Exception {
        JsonNode node = mapper.readTree("{\"userId\":\"${user.id}\"}");
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.USER_ID, "user-123");

        service.replacePlaceholders(node, request);

        assertEquals("user-123", node.get("userId").asText());
    }

    @Test
    void replacePlaceholdersShouldLeaveNonPlaceholderTextUnchanged() throws Exception {
        JsonNode node = mapper.readTree("{\"title\":\"Plain Text\"}");
        Map<String, Object> request = new HashMap<>();

        service.replacePlaceholders(node, request);

        assertEquals("Plain Text", node.get("title").asText());
    }

    @Test
    void replacePlaceholdersShouldRecurseIntoNestedObjects() throws Exception {
        JsonNode node = mapper.readTree("{\"nested\":{\"courseId\":\"${course.id}\"}}");
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.COURSE_ID, "course-99");

        service.replacePlaceholders(node, request);

        assertEquals("course-99", node.get("nested").get("courseId").asText());
    }

    @Test
    void replacePlaceholdersShouldRecurseIntoArraysOfObjects() throws Exception {
        JsonNode node = mapper.readTree("{\"items\":[{\"courseName\":\"${course.name}\"}]}");
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.COURSE_NAME, "Intro to Testing");

        service.replacePlaceholders(node, request);

        assertEquals("Intro to Testing", node.get("items").get(0).get("courseName").asText());
    }

    @Test
    void replacePlaceholdersShouldResolveSvgTemplateFromServerProperties() throws Exception {
        lenient().when(serverProperties.getSvgTemplate()).thenReturn("<svg></svg>");
        JsonNode node = mapper.readTree("{\"template\":\"${svgTemplate}\"}");
        Map<String, Object> request = new HashMap<>();

        service.replacePlaceholders(node, request);

        assertEquals("<svg></svg>", node.get("template").asText());
    }

    @Test
    void replacePlaceholdersShouldResolveUnknownPlaceholderToEmptyString() throws Exception {
        JsonNode node = mapper.readTree("{\"value\":\"${unknown.placeholder}\"}");
        Map<String, Object> request = new HashMap<>();

        service.replacePlaceholders(node, request);

        assertEquals("", node.get("value").asText());
    }

    @Test
    void convertDateFormatShouldReformatDateViaReflection() throws Exception {
        Method method = KafkaCertificateProducerServiceImpl.class
                .getDeclaredMethod("convertDateFormat", String.class);
        method.setAccessible(true);
        Object result = method.invoke(null, "03/05/2024");
        assertEquals("2024-03-05", result);
    }
}
