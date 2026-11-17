package com.assessment.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationAsyncRequestTest {

    @Test
    void shouldGetAndSetAllFields() {
        NotificationAsyncRequest request = new NotificationAsyncRequest();
        Map<String, Object> action = new HashMap<>();
        action.put("key", "value");
        List<String> ids = Collections.singletonList("id1");
        List<String> copyEmail = Collections.singletonList("cc@example.com");

        request.setType("email");
        request.setPriority(1);
        request.setAction(action);
        request.setIds(ids);
        request.setCopyEmail(copyEmail);

        assertEquals("email", request.getType());
        assertEquals(1, request.getPriority());
        assertEquals(action, request.getAction());
        assertEquals(ids, request.getIds());
        assertEquals(copyEmail, request.getCopyEmail());
    }
}
