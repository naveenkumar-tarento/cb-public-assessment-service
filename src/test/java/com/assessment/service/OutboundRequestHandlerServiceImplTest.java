package com.assessment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundRequestHandlerServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    private final OutboundRequestHandlerServiceImpl service = new OutboundRequestHandlerServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    private HttpClientErrorException clientError(String body) {
        return new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request", new HttpHeaders(),
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    @Test
    void fetchResultUsingPostShouldReturnResponseOnSuccess() {
        Map<String, Object> responseBody = Collections.singletonMap("status", "ok");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(responseBody);

        Object result = service.fetchResultUsingPost("http://api/endpoint", Collections.singletonMap("key", "value"));

        assertEquals(responseBody, result);
    }

    @Test
    void fetchResultUsingPostShouldParseErrorBodyOnHttpClientErrorException() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(clientError("{\"error\":\"bad request\"}"));

        Object result = service.fetchResultUsingPost("http://api/endpoint", Collections.singletonMap("key", "value"));

        assertTrue(result instanceof Map);
        assertEquals("bad request", ((Map<?, ?>) result).get("error"));
    }

    @Test
    void fetchResultUsingPostShouldReturnNullOnGenericException() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("network issue"));

        Object result = service.fetchResultUsingPost("http://api/endpoint", Collections.singletonMap("key", "value"));

        assertNull(result);
    }

    @Test
    void fetchResultShouldReturnResponseOnSuccess() {
        Map<String, Object> responseBody = Collections.singletonMap("status", "ok");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(responseBody);

        Object result = service.fetchResult("http://api/endpoint");

        assertEquals(responseBody, result);
    }

    @Test
    void fetchResultShouldParseErrorBodyOnHttpClientErrorException() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(clientError("{\"error\":\"not found\"}"));

        Object result = service.fetchResult("http://api/endpoint");

        assertTrue(result instanceof Map);
        assertEquals("not found", ((Map<?, ?>) result).get("error"));
    }

    @Test
    void fetchResultShouldReturnNullOnGenericException() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenThrow(new RuntimeException("boom"));

        Object result = service.fetchResult("http://api/endpoint");

        assertNull(result);
    }

    @Test
    void fetchUsingGetWithHeadersShouldReturnBodyOnSuccess() {
        Map<String, Object> body = Collections.singletonMap("key", "value");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");

        Object result = service.fetchUsingGetWithHeaders("http://api/endpoint", headers);

        assertEquals(body, result);
    }

    @Test
    void fetchUsingGetWithHeadersShouldReturnNullOnException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("boom"));

        Object result = service.fetchUsingGetWithHeaders("http://api/endpoint", Collections.emptyMap());

        assertNull(result);
    }

    @Test
    void fetchUsingGetWithHeadersShouldReturnNullOnHttpClientError() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(clientError("{\"error\":\"forbidden\"}"));

        Object result = service.fetchUsingGetWithHeaders("http://api/endpoint", null);

        assertNull(result);
    }

    @Test
    void fetchUsingGetWithHeadersProfileShouldReturnBodyOnSuccess() {
        Map<String, Object> body = Collections.singletonMap("key", "value");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        Object result = service.fetchUsingGetWithHeadersProfile("http://api/endpoint", Collections.emptyMap());

        assertEquals(body, result);
    }

    @Test
    void fetchUsingGetWithHeadersProfileShouldParseErrorBodyOnHttpClientError() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(clientError("{\"error\":\"forbidden\"}"));

        Object result = service.fetchUsingGetWithHeadersProfile("http://api/endpoint", Collections.emptyMap());

        assertTrue(result instanceof Map);
        assertEquals("forbidden", ((Map<?, ?>) result).get("error"));
    }

    @Test
    void fetchResultUsingPostWithHeadersShouldReturnResponseOnSuccess() {
        Map<String, Object> responseBody = Collections.singletonMap("status", "ok");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(responseBody);

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Custom", "value");

        Map<String, Object> result = service.fetchResultUsingPost("http://api/endpoint",
                Collections.singletonMap("key", "value"), headers);

        assertEquals(responseBody, result);
    }

    @Test
    void fetchResultUsingPostWithHeadersShouldParseErrorBodyOnHttpClientError() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(clientError("{\"error\":\"conflict\"}"));

        Map<String, Object> result = service.fetchResultUsingPost("http://api/endpoint",
                Collections.singletonMap("key", "value"), Collections.emptyMap());

        assertEquals("conflict", result.get("error"));
    }

    @Test
    void fetchResultUsingPatchShouldReturnResponseOnSuccess() {
        Map<String, Object> responseBody = Collections.singletonMap("status", "ok");
        when(restTemplate.patchForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(responseBody);

        Map<String, Object> result = service.fetchResultUsingPatch("http://api/endpoint",
                Collections.singletonMap("key", "value"), Collections.emptyMap());

        assertEquals(responseBody, result);
    }

    @Test
    void fetchResultUsingPatchShouldReturnEmptyMapWhenResponseNullAndNoError() {
        when(restTemplate.patchForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(null);

        Map<String, Object> result = service.fetchResultUsingPatch("http://api/endpoint",
                Collections.singletonMap("key", "value"), Collections.emptyMap());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchResultUsingPatchShouldParseErrorBodyOnHttpClientError() {
        when(restTemplate.patchForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(clientError("{\"error\":\"patch failed\"}"));

        Map<String, Object> result = service.fetchResultUsingPatch("http://api/endpoint",
                Collections.singletonMap("key", "value"), Collections.emptyMap());

        assertEquals("patch failed", result.get("error"));
    }
}
