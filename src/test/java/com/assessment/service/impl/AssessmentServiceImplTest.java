package com.assessment.service.impl;

import com.assessment.datasecurity.EncryptionService;
import com.assessment.model.SBApiResponse;
import com.assessment.repo.AssessmentRepository;
import com.assessment.service.AssessmentUtilServiceV2;
import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceImplTest {

    @Mock
    private AssessmentUtilServiceV2 assessmentUtilService;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private ServerProperties serverProperties;

    @Mock
    private AssessmentRepository assessmentRepository;

    private final AssessmentServiceImpl service = new AssessmentServiceImpl();
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "assessmentUtilService", assessmentUtilService);
        ReflectionTestUtils.setField(service, "encryptionService", encryptionService);
        ReflectionTestUtils.setField(service, "serverProperties", serverProperties);
        ReflectionTestUtils.setField(service, "assessmentRepository", assessmentRepository);
        ReflectionTestUtils.setField(service, "mapper", mapper);

        lenient().when(encryptionService.encryptData(anyString())).thenReturn("encrypted-email");
        lenient().when(serverProperties.getAssessmentLevelParams())
                .thenReturn(Arrays.asList("identifier", "primaryCategory", "expectedDuration"));
        lenient().when(serverProperties.getAssessmentSectionParams()).thenReturn(Arrays.asList("identifier"));
    }

    private Map<String, Object> requestBody(String email, String assessmentId, String contextId) {
        Map<String, Object> body = new HashMap<>();
        body.put(Constants.EMAIL, email);
        body.put(Constants.NAME, "John Doe");
        body.put(Constants.ASSESSMENT_IDENTIFIER, assessmentId);
        body.put(Constants.CONTEXT_ID, contextId);
        return body;
    }

    private Map<String, Object> hierarchyWithSections(String primaryCategory, Integer expectedDuration) {
        Map<String, Object> question = new HashMap<>();
        question.put(Constants.IDENTIFIER, "q1");

        Map<String, Object> section = new HashMap<>();
        section.put(Constants.IDENTIFIER, "s1");
        section.put(Constants.CHILDREN, Collections.singletonList(question));

        Map<String, Object> hierarchy = new HashMap<>();
        hierarchy.put(Constants.PRIMARY_CATEGORY, primaryCategory);
        hierarchy.put(Constants.CHILDREN, Collections.singletonList(section));
        if (expectedDuration != null) {
            hierarchy.put(Constants.EXPECTED_DURATION, expectedDuration);
        }
        return hierarchy;
    }

    @Test
    void readAssessmentShouldReturnBadRequestForInvalidEmail() {
        SBApiResponse response = service.readAssessment(false, requestBody("not-an-email", "assess1", "ctx1"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.INVALID_EMAIL, response.getParams().getErrmsg());
    }

    @Test
    void readAssessmentShouldReturnServerErrorWhenHierarchyEmpty() {
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(Collections.emptyMap());

        SBApiResponse response = service.readAssessment(false, requestBody("user@example.com", "assess1", "ctx1"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.ASSESSMENT_HIERARCHY_READ_FAILED, response.getParams().getErrmsg());
    }

    @Test
    void readAssessmentShouldReturnQuestionSetForPracticeQuestionSet() {
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(hierarchyWithSections(Constants.PRACTICE_QUESTION_SET, 60));

        SBApiResponse response = service.readAssessment(false, requestBody("user@example.com", "assess1", "ctx1"));

        assertTrue(response.getResult().containsKey(Constants.QUESTION_SET));
    }

    @Test
    void readAssessmentShouldUseFetchHierarchyWhenEditModeTrue() {
        when(assessmentUtilService.fetchHierarchyFromAssessServc(anyString()))
                .thenReturn(hierarchyWithSections("Assessment", 60));

        SBApiResponse response = service.readAssessment(true, requestBody("user@example.com", "assess1", "ctx1"));

        assertTrue(response.getResult().containsKey(Constants.QUESTION_SET));
    }

    @Test
    void readAssessmentShouldReturnErrorWhenExpectedDurationMissingAndNoExistingData() {
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(hierarchyWithSections("Assessment", null));
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());

        SBApiResponse response = service.readAssessment(false, requestBody("user@example.com", "assess1", "ctx1"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.ASSESSMENT_INVALID, response.getParams().getErrmsg());
    }

    @Test
    void readAssessmentShouldAddDataToDbWhenNoExistingDataAndExpectedDurationPresent() {
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(hierarchyWithSections("Assessment", 60));
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());
        when(assessmentRepository.addUserAssesmentDataToDB(anyString(), anyString(), any(), any(), any(), anyString(),
                anyString(), any())).thenReturn(true);

        SBApiResponse response = service.readAssessment(false, requestBody("user@example.com", "assess1", "ctx1"));

        assertTrue(response.getResult().containsKey(Constants.QUESTION_SET));
        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void readAssessmentShouldSetErrorWhenAddDataToDbFails() {
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(hierarchyWithSections("Assessment", 60));
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());
        when(assessmentRepository.addUserAssesmentDataToDB(anyString(), anyString(), any(), any(), any(), anyString(),
                anyString(), any())).thenReturn(false);

        SBApiResponse response = service.readAssessment(false, requestBody("user@example.com", "assess1", "ctx1"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.ASSESSMENT_DATA_START_TIME_NOT_UPDATED, response.getParams().getErrmsg());
    }

    @Test
    void readAssessmentShouldReturnExistingInProgressQuestionSet() {
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(hierarchyWithSections("Assessment", 60));

        Date futureEndTime = new Date(System.currentTimeMillis() + 3600_000L);
        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.END_TIME, futureEndTime);
        existing.put(Constants.STATUS, Constants.NOT_SUBMITTED);
        existing.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, "{\"existingKey\":\"existingValue\"}");
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));

        SBApiResponse response = service.readAssessment(false, requestBody("user@example.com", "assess1", "ctx1"));

        Map<String, Object> questionSet = (Map<String, Object>) response.getResult().get(Constants.QUESTION_SET);
        assertEquals("existingValue", questionSet.get("existingKey"));
    }

    @Test
    void readAssessmentShouldReturnAlreadySubmittedMessageWhenPassed() {
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(hierarchyWithSections("Assessment", 60));

        Date pastEndTime = new Date(System.currentTimeMillis() - 3600_000L);
        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.END_TIME, pastEndTime);
        existing.put(Constants.STATUS, Constants.SUBMITTED);
        existing.put(Constants.PASS_STATUS, true);
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));

        SBApiResponse response = service.readAssessment(false, requestBody("user@example.com", "assess1", "ctx1"));

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals("User has already submitted the assessment", response.getResult().get(Constants.RESPONSE));
    }

    @Test
    void readAssessmentShouldRetryWhenSubmittedButNotPassed() {
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(hierarchyWithSections("Assessment", 60));

        Date pastEndTime = new Date(System.currentTimeMillis() - 3600_000L);
        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.END_TIME, pastEndTime);
        existing.put(Constants.STATUS, Constants.SUBMITTED);
        existing.put(Constants.PASS_STATUS, false);
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));
        when(assessmentRepository.addUserAssesmentDataToDB(anyString(), anyString(), any(), any(), any(), anyString(),
                anyString(), any())).thenReturn(true);

        SBApiResponse response = service.readAssessment(false, requestBody("user@example.com", "assess1", "ctx1"));

        assertTrue(response.getResult().containsKey(Constants.QUESTION_SET));
    }

    @Test
    void readAssessmentShouldHandleExceptionGracefully() {
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("cache down"));

        SBApiResponse response = service.readAssessment(false, requestBody("user@example.com", "assess1", "ctx1"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    @Test
    void readQuestionListShouldReturnBadRequestWhenAssessmentIdMissing() {
        Map<String, Object> body = new HashMap<>();
        body.put(Constants.EMAIL, "user@example.com");

        SBApiResponse response = service.readQuestionList(body, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.ASSESSMENT_ID_KEY_IS_NOT_PRESENT_IS_EMPTY, response.getParams().getErrmsg());
    }

    @Test
    void readQuestionListShouldReturnBadRequestWhenIdentifierListEmpty() {
        Map<String, Object> body = new HashMap<>();
        body.put(Constants.EMAIL, "user@example.com");
        body.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");

        SBApiResponse response = service.readQuestionList(body, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.IDENTIFIER_LIST_IS_EMPTY, response.getParams().getErrmsg());
    }

    private Map<String, Object> questionListRequestBody(String assessmentId, List<String> ids) {
        Map<String, Object> body = new HashMap<>();
        body.put(Constants.EMAIL, "user@example.com");
        body.put(Constants.ASSESSMENT_IDENTIFIER, assessmentId);

        Map<String, Object> identifierFilter = new HashMap<>();
        identifierFilter.put(Constants.IDENTIFIER, ids);

        Map<String, Object> search = new HashMap<>();
        search.put(Constants.SEARCH, identifierFilter);
        body.put(Constants.REQUEST, search);
        return body;
    }

    @Test
    void readQuestionListShouldReturnServerErrorWhenHierarchyEmpty() {
        Map<String, Object> body = questionListRequestBody("assess1", Collections.singletonList("q1"));
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(Collections.emptyMap());

        SBApiResponse response = service.readQuestionList(body, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.ASSESSMENT_HIERARCHY_READ_FAILED, response.getParams().getErrmsg());
    }

    @Test
    void readQuestionListShouldReturnQuestionsForPracticeQuestionSet() throws Exception {
        Map<String, Object> body = questionListRequestBody("assess1", Collections.singletonList("q1"));
        Map<String, Object> hierarchy = new HashMap<>();
        hierarchy.put(Constants.PRIMARY_CATEGORY, Constants.PRACTICE_QUESTION_SET);
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hierarchy);

        Map<String, Object> questionsMap = new HashMap<>();
        questionsMap.put("q1", Collections.singletonMap("id", "q1"));
        when(assessmentUtilService.readQListfromCache(any(), anyString(), anyBoolean())).thenReturn(questionsMap);
        when(assessmentUtilService.filterQuestionMapDetail(any(), any())).thenReturn(Collections.singletonMap("id", "q1"));

        SBApiResponse response = service.readQuestionList(body, false);

        assertTrue(response.getResult().containsKey(Constants.QUESTIONS));
    }

    @Test
    void readQuestionListShouldReturnErrorWhenUserAssessmentDataNotPresent() {
        Map<String, Object> body = questionListRequestBody("assess1", Collections.singletonList("q1"));
        Map<String, Object> hierarchy = new HashMap<>();
        hierarchy.put(Constants.PRIMARY_CATEGORY, "Assessment");
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hierarchy);
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());

        SBApiResponse response = service.readQuestionList(body, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.USER_ASSESSMENT_DATA_NOT_PRESENT, response.getParams().getErrmsg());
    }

    @Test
    void readQuestionListShouldSucceedWhenIdentifiersMatchUserAssessment() throws Exception {
        Map<String, Object> body = questionListRequestBody("assess1", Collections.singletonList("q1"));
        Map<String, Object> hierarchy = new HashMap<>();
        hierarchy.put(Constants.PRIMARY_CATEGORY, "Assessment");
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hierarchy);

        Map<String, Object> existing = new HashMap<>();
        Map<String, Object> section = new HashMap<>();
        section.put(Constants.CHILD_NODES, Collections.singletonList("q1"));
        Map<String, Object> userAssessmentDetail = new HashMap<>();
        userAssessmentDetail.put(Constants.PRIMARY_CATEGORY, "Assessment");
        userAssessmentDetail.put(Constants.CHILDREN, Collections.singletonList(section));

        existing.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, mapper.writeValueAsString(userAssessmentDetail));
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));

        Map<String, Object> questionsMap = new HashMap<>();
        questionsMap.put("q1", Collections.singletonMap("id", "q1"));
        when(assessmentUtilService.readQListfromCache(any(), anyString(), anyBoolean())).thenReturn(questionsMap);
        when(assessmentUtilService.filterQuestionMapDetail(any(), any())).thenReturn(Collections.singletonMap("id", "q1"));

        SBApiResponse response = service.readQuestionList(body, false);

        assertTrue(response.getResult().containsKey(Constants.QUESTIONS));
    }
}
