package com.assessment.controller;

import com.assessment.model.SBApiResponse;
import com.assessment.service.AssessmentService;
import com.assessment.service.AssessmentServiceV4;
import com.assessment.service.AssessmentServiceV5;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentControllerTest {

    @Mock
    private AssessmentService assessmentService;

    @Mock
    private AssessmentServiceV4 assessmentServiceV4;

    @Mock
    private AssessmentServiceV5 assessmentServiceV5;

    private AssessmentController controller;

    @BeforeEach
    void setUp() {
        controller = new AssessmentController();
        ReflectionTestUtils.setField(controller, "assessmentService", assessmentService);
        ReflectionTestUtils.setField(controller, "assessmentServiceV4", assessmentServiceV4);
        ReflectionTestUtils.setField(controller, "assessmentServiceV5", assessmentServiceV5);
    }

    private SBApiResponse okResponse() {
        SBApiResponse response = new SBApiResponse();
        response.setResponseCode(HttpStatus.OK);
        return response;
    }

    @Test
    void readAssessmentV4ShouldDefaultEditModeToFalseWhenNull() {
        Map<String, Object> request = Collections.singletonMap("key", "value");
        when(assessmentService.readAssessment(anyBoolean(), anyMap())).thenReturn(okResponse());

        ResponseEntity<SBApiResponse> response = controller.readAssessmentV4(request, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(assessmentService).readAssessment(false, request);
    }

    @Test
    void readAssessmentV4ShouldParseEditModeTrue() {
        Map<String, Object> request = Collections.singletonMap("key", "value");
        when(assessmentService.readAssessment(anyBoolean(), anyMap())).thenReturn(okResponse());

        controller.readAssessmentV4(request, "true");

        verify(assessmentService).readAssessment(true, request);
    }

    @Test
    void readQuestionListV4ShouldDelegateToAssessmentService() {
        Map<String, Object> request = Collections.singletonMap("key", "value");
        when(assessmentService.readQuestionList(anyMap(), anyBoolean())).thenReturn(okResponse());

        ResponseEntity<SBApiResponse> response = controller.readQuestionListV4(request, "false");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(assessmentService).readQuestionList(request, false);
    }

    @Test
    void submitUserAssessmentV4ShouldDelegateToAssessmentServiceV4() {
        Map<String, Object> request = Collections.singletonMap("key", "value");
        when(assessmentServiceV4.submitAssessmentAsync(anyMap(), anyBoolean())).thenReturn(okResponse());

        ResponseEntity<SBApiResponse> response = controller.submitUserAssessmentV4(request, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(assessmentServiceV4).submitAssessmentAsync(request, false);
    }

    @Test
    void submitUserAssessmentV5ShouldDelegateToAssessmentServiceV5() {
        Map<String, Object> request = Collections.singletonMap("key", "value");
        when(assessmentServiceV5.submitAssessmentAsync(anyMap(), anyBoolean())).thenReturn(okResponse());

        ResponseEntity<SBApiResponse> response = controller.submitUserAssessmentV5(request, "true");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(assessmentServiceV5).submitAssessmentAsync(request, true);
    }

    @Test
    void readAssessmentV5ShouldDefaultEditModeToFalseWhenNull() {
        Map<String, Object> request = Collections.singletonMap("key", "value");
        when(assessmentServiceV5.readAssessment(anyBoolean(), anyMap())).thenReturn(okResponse());

        controller.readAssessmentV5(request, null);

        verify(assessmentServiceV5).readAssessment(false, request);
    }

    @Test
    void readAssessmentV5ShouldParseEditModeTrue() {
        Map<String, Object> request = Collections.singletonMap("key", "value");
        when(assessmentServiceV5.readAssessment(anyBoolean(), anyMap())).thenReturn(okResponse());

        controller.readAssessmentV5(request, "true");

        verify(assessmentServiceV5).readAssessment(true, request);
    }

    @Test
    void readQuestionListV5ShouldDelegateToAssessmentServiceV5() {
        Map<String, Object> request = Collections.singletonMap("key", "value");
        when(assessmentServiceV5.readQuestionList(anyMap(), anyBoolean())).thenReturn(okResponse());

        ResponseEntity<SBApiResponse> response = controller.readQuestionListV5(request, "true");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(assessmentServiceV5).readQuestionList(request, true);
    }

    @Test
    void readAssessmentResultV5ShouldDelegateToAssessmentServiceV5() {
        Map<String, Object> request = Collections.singletonMap("key", "value");
        when(assessmentServiceV5.readAssessmentResultV5(anyMap())).thenReturn(okResponse());

        ResponseEntity<SBApiResponse> response = controller.readAssessmentResultV5(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(assessmentServiceV5).readAssessmentResultV5(request);
    }

    @Test
    void assessmentCertificateReissueShouldDelegateToAssessmentServiceV5() {
        Map<String, Object> request = Collections.singletonMap("key", "value");
        when(assessmentServiceV5.assessmentCertificateReissue(anyMap())).thenReturn(okResponse());

        ResponseEntity<SBApiResponse> response = controller.assessmentCertificateReissue(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(assessmentServiceV5).assessmentCertificateReissue(request);
    }
}
