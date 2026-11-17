package com.assessment;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PublicAssessmentServiceApplicationTest {

    @Test
    void restTemplateBeanShouldNotBeNull() throws Exception {
        PublicAssessmentServiceApplication application = new PublicAssessmentServiceApplication();
        RestTemplate restTemplate = application.restTemplate();
        assertNotNull(restTemplate);
        assertNotNull(restTemplate.getRequestFactory());
    }
}
