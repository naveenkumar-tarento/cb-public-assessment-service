package com.assessment.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateTest {

    @Test
    void constructorShouldSetAllFields() {
        Map<String, Object> params = Collections.singletonMap("key", "value");
        Template template = new Template("data", "id1", params);

        assertEquals("data", template.getData());
        assertEquals("id1", template.getId());
        assertEquals(params, template.getParams());
    }

    @Test
    void shouldGetAndSetAllFields() {
        Template template = new Template("data", "id1", new HashMap<>());
        template.setData("newData");
        template.setId("newId");
        Map<String, Object> newParams = Collections.singletonMap("k", "v");
        template.setParams(newParams);

        assertEquals("newData", template.getData());
        assertEquals("newId", template.getId());
        assertEquals(newParams, template.getParams());
    }

    @Test
    void toStringShouldContainFieldValues() {
        Template template = new Template("data", "id1", Collections.emptyMap());
        String result = template.toString();
        assertTrue(result.contains("data"));
        assertTrue(result.contains("id1"));
    }
}
