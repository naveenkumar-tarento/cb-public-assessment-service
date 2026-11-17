package com.assessment.cache.controller;

import com.assessment.cache.service.RedisCacheService;
import com.assessment.model.SBApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCacheControllerTest {

    @Mock
    private RedisCacheService redisCacheService;

    private RedisCacheController controller;

    @Test
    void deleteCacheShouldReturnServiceResponse() throws Exception {
        controller = new RedisCacheController();
        controller.redisCacheService = redisCacheService;

        SBApiResponse response = new SBApiResponse();
        response.setResponseCode(HttpStatus.OK);
        when(redisCacheService.deleteCache()).thenReturn(response);

        ResponseEntity<?> result = controller.deleteCache();
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void getKeysShouldReturnServiceResponse() throws Exception {
        controller = new RedisCacheController();
        controller.redisCacheService = redisCacheService;

        SBApiResponse response = new SBApiResponse();
        response.setResponseCode(HttpStatus.NOT_FOUND);
        when(redisCacheService.getKeys()).thenReturn(response);

        ResponseEntity<?> result = controller.getKeys();
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void getKeysAndValuesShouldReturnServiceResponse() throws Exception {
        controller = new RedisCacheController();
        controller.redisCacheService = redisCacheService;

        SBApiResponse response = new SBApiResponse();
        response.setResponseCode(HttpStatus.OK);
        when(redisCacheService.getKeysAndValues()).thenReturn(response);

        ResponseEntity<?> result = controller.getKeysAndValues();
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
