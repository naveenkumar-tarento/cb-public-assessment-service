package com.assessment.model;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SBApiResponseTest {

    @Test
    void defaultConstructorShouldInitializeVersionTimestampAndParams() {
        SBApiResponse response = new SBApiResponse();
        assertEquals("v1", response.getVer());
        assertNotNull(response.getTs());
        assertNotNull(response.getParams());
        assertNotNull(response.getParams().getResmsgid());
    }

    @Test
    void idConstructorShouldSetId() {
        SBApiResponse response = new SBApiResponse("resp-1");
        assertEquals("resp-1", response.getId());
        assertEquals("v1", response.getVer());
    }

    @Test
    void shouldGetAndSetAllFields() {
        SBApiResponse response = new SBApiResponse();
        response.setId("id1");
        response.setVer("v2");
        response.setTs("ts1");
        SunbirdApiRespParam params = new SunbirdApiRespParam("rid");
        response.setParams(params);
        response.setResponseCode(HttpStatus.OK);

        assertEquals("id1", response.getId());
        assertEquals("v2", response.getVer());
        assertEquals("ts1", response.getTs());
        assertEquals(params, response.getParams());
        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void shouldManageResultMap() {
        SBApiResponse response = new SBApiResponse();
        response.put("key1", "value1");
        assertEquals("value1", response.get("key1"));
        assertTrue(response.containsKey("key1"));
        assertFalse(response.containsKey("missing"));

        Map<String, Object> extra = new HashMap<>();
        extra.put("key2", "value2");
        response.putAll(extra);
        assertEquals("value2", response.get("key2"));

        Map<String, Object> newResult = new HashMap<>();
        newResult.put("key3", "value3");
        response.setResult(newResult);
        assertEquals(newResult, response.getResult());
    }
}
