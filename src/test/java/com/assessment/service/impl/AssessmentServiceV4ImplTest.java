package com.assessment.service.impl;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.datasecurity.DecryptionService;
import com.assessment.datasecurity.EncryptionService;
import com.assessment.kafka.Producer;
import com.assessment.kafka.service.KafkaCertificateProducerService;
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
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
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
class AssessmentServiceV4ImplTest {

    @Mock
    private AssessmentUtilServiceV2 assessmentUtilService;

    @Mock
    private ServerProperties serverProperties;

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private DecryptionService decryptionService;

    @Mock
    private KafkaCertificateProducerService kafkaCertificateProducerService;

    @Mock
    private Producer producer;

    private final AssessmentServiceV4Impl service = new AssessmentServiceV4Impl();
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "assessmentUtilService", assessmentUtilService);
        ReflectionTestUtils.setField(service, "serverProperties", serverProperties);
        ReflectionTestUtils.setField(service, "assessmentRepository", assessmentRepository);
        ReflectionTestUtils.setField(service, "encryptionService", encryptionService);
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "cassandraOperation", cassandraOperation);
        ReflectionTestUtils.setField(service, "resourceLoader", resourceLoader);
        ReflectionTestUtils.setField(service, "decryptionService", decryptionService);
        ReflectionTestUtils.setField(service, "kafkaCertificateProducerService", kafkaCertificateProducerService);
        ReflectionTestUtils.setField(service, "producer", producer);

        lenient().when(encryptionService.encryptData(anyString())).thenReturn("encrypted-email");
    }

    private Map<String, Object> submitRequest(String assessmentId, String email, List<Map<String, Object>> sections) {
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.EMAIL, email);
        request.put(Constants.IDENTIFIER, assessmentId);
        request.put(Constants.CONTEXT_ID, "ctx1");
        request.put(Constants.CHILDREN, sections);
        request.put(Constants.COURSE_ID, "course1");
        return request;
    }

    private Map<String, Object> hierarchySection(String id, int minPassPercentage) {
        Map<String, Object> section = new HashMap<>();
        section.put(Constants.IDENTIFIER, id);
        section.put(Constants.OBJECT_TYPE, "AssessmentSection");
        section.put(Constants.PRIMARY_CATEGORY, "Assessment");
        section.put(Constants.MINIMUM_PASS_PERCENTAGE, minPassPercentage);
        section.put(Constants.CHILDREN, Collections.singletonList("q1"));
        return section;
    }

    private Map<String, Object> submitSection(String id) {
        Map<String, Object> section = new HashMap<>();
        section.put(Constants.IDENTIFIER, id);
        Map<String, Object> question = new HashMap<>();
        question.put(Constants.IDENTIFIER, "q1");
        section.put(Constants.CHILDREN, Collections.singletonList(question));
        return section;
    }

    private Map<String, Object> hierarchy(String primaryCategory, String scoreCutOffType,
                                           List<Map<String, Object>> sections) {
        Map<String, Object> h = new HashMap<>();
        h.put(Constants.PRIMARY_CATEGORY, primaryCategory);
        h.put(Constants.SCORE_CUTOFF_TYPE, scoreCutOffType);
        h.put(Constants.CHILDREN, sections);
        return h;
    }

    private Map<String, Object> qumlResult(double result, int total, int blank, int correct, int incorrect) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(Constants.RESULT, result);
        resultMap.put(Constants.TOTAL, total);
        resultMap.put(Constants.BLANK, blank);
        resultMap.put(Constants.CORRECT, correct);
        resultMap.put(Constants.INCORRECT, incorrect);
        return resultMap;
    }

    @Test
    void submitAssessmentAsyncShouldReturnBadRequestForInvalidEmail() {
        Map<String, Object> request = submitRequest("assess1", "not-an-email", Collections.emptyList());
        SBApiResponse response = service.submitAssessmentAsync(request, true);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.INVALID_EMAIL, response.getParams().getErrmsg());
    }

    @Test
    void submitAssessmentAsyncShouldReturnBadRequestForMissingIdentifier() {
        Map<String, Object> request = submitRequest(null, "user@example.com", Collections.emptyList());
        request.remove(Constants.IDENTIFIER);
        SBApiResponse response = service.submitAssessmentAsync(request, true);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.INVALID_ASSESSMENT_ID, response.getParams().getErrmsg());
    }

    @Test
    void submitAssessmentAsyncShouldReturnBadRequestWhenHierarchyEmpty() {
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(Collections.emptyMap());

        Map<String, Object> request = submitRequest("assess1", "user@example.com", Collections.emptyList());
        SBApiResponse response = service.submitAssessmentAsync(request, true);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.READ_ASSESSMENT_FAILED, response.getParams().getErrmsg());
    }

    @Test
    void submitAssessmentAsyncShouldComputeAssessmentLevelResultInEditMode() throws Exception {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        Map<String, Object> hier = hierarchy("Assessment", Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF,
                Collections.singletonList(hSection));
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);
        when(assessmentUtilService.readQListfromCache(any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptyMap());
        when(assessmentUtilService.validateQumlAssessment(any(), any(), any()))
                .thenReturn(qumlResult(75.0, 10, 1, 7, 2));

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("s1")));
        SBApiResponse response = service.submitAssessmentAsync(request, true);

        assertEquals("Assessment", response.getResult().get(Constants.PRIMARY_CATEGORY));
        assertEquals(75.0, response.getResult().get(Constants.OVERALL_RESULT));
        assertEquals(Boolean.TRUE, response.getResult().get(Constants.PASS));
    }

    @Test
    void submitAssessmentAsyncShouldComputeSectionLevelResultInEditMode() throws Exception {
        Map<String, Object> hSection1 = hierarchySection("s1", 50);
        Map<String, Object> hSection2 = hierarchySection("s2", 50);
        Map<String, Object> hier = hierarchy("Assessment", Constants.SECTION_LEVEL_SCORE_CUTOFF,
                Arrays.asList(hSection1, hSection2));
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);
        when(assessmentUtilService.readQListfromCache(any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptyMap());
        when(assessmentUtilService.validateQumlAssessment(any(), any(), any()))
                .thenReturn(qumlResult(80.0, 10, 1, 8, 1))
                .thenReturn(qumlResult(40.0, 10, 4, 4, 2));

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Arrays.asList(submitSection("s1"), submitSection("s2")));
        SBApiResponse response = service.submitAssessmentAsync(request, true);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(60.0, response.getResult().get(Constants.OVERALL_RESULT));
        assertEquals(Boolean.FALSE, response.getResult().get(Constants.PASS));
    }

    @Test
    void submitAssessmentAsyncShouldReturnDefaultResponseForUnknownScoreCutOffType() {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        Map<String, Object> hier = hierarchy("Assessment", "unknowntype", Collections.singletonList(hSection));
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("s1")));
        SBApiResponse response = service.submitAssessmentAsync(request, true);

        assertTrue(response.getResult().isEmpty());
    }

    @Test
    void submitAssessmentAsyncShouldHandleExceptionGracefully() {
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("cache down"));

        Map<String, Object> request = submitRequest("assess1", "user@example.com", Collections.emptyList());
        SBApiResponse response = service.submitAssessmentAsync(request, true);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    @Test
    void submitAssessmentAsyncShouldReturnUserAssessmentDataNotPresentWhenNoExistingRecordsAndNotEditMode() {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        Map<String, Object> hier = hierarchy("Assessment", Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF,
                Collections.singletonList(hSection));
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("s1")));
        SBApiResponse response = service.submitAssessmentAsync(request, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.USER_ASSESSMENT_DATA_NOT_PRESENT, response.getParams().getErrmsg());
    }

    @Test
    void submitAssessmentAsyncShouldReturnStartTimeFailedWhenMissing() {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        Map<String, Object> hier = hierarchy("Assessment", Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF,
                Collections.singletonList(hSection));
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);
        Map<String, Object> existing = new HashMap<>();
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("s1")));
        SBApiResponse response = service.submitAssessmentAsync(request, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.READ_ASSESSMENT_START_TIME_FAILED, response.getParams().getErrmsg());
    }

    @Test
    void submitAssessmentAsyncShouldReturnExpiredWhenSubmissionTimeTooLate() {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        hSection.put(Constants.EXPECTED_DURATION, 60);
        Map<String, Object> hier = hierarchy("Assessment", Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF,
                Collections.singletonList(hSection));
        hier.put(Constants.EXPECTED_DURATION, 60);
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.START_TIME, new Date(System.currentTimeMillis() - 10 * 60 * 1000L));
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));
        when(serverProperties.getUserAssessmentSubmissionDuration()).thenReturn("10");

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("s1")));
        SBApiResponse response = service.submitAssessmentAsync(request, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.ASSESSMENT_SUBMIT_EXPIRED, response.getParams().getErrmsg());
    }

    @Test
    void submitAssessmentAsyncShouldReturnWrongSectionDetailsWhenSectionsMismatch() {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        hier_SetExpectedDuration(hSection);
        Map<String, Object> hier = hierarchy("Assessment", Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF,
                Collections.singletonList(hSection));
        hier.put(Constants.EXPECTED_DURATION, 3600);
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.START_TIME, new Date());
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));
        when(serverProperties.getUserAssessmentSubmissionDuration()).thenReturn("10");

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("wrongSectionId")));
        SBApiResponse response = service.submitAssessmentAsync(request, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.WRONG_SECTION_DETAILS, response.getParams().getErrmsg());
    }

    private void hier_SetExpectedDuration(Map<String, Object> hSection) {
        hSection.put(Constants.EXPECTED_DURATION, 3600);
    }

    @Test
    void submitAssessmentAsyncShouldReturnQuestionReadFailedWhenNoPreviousQuestionSet() {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        Map<String, Object> hier = hierarchy("Assessment", Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF,
                Collections.singletonList(hSection));
        hier.put(Constants.EXPECTED_DURATION, 3600);
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.START_TIME, new Date());
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));
        when(serverProperties.getUserAssessmentSubmissionDuration()).thenReturn("10");

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("s1")));
        SBApiResponse response = service.submitAssessmentAsync(request, false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.ASSESSMENT_SUBMIT_QUESTION_READ_FAILED, response.getParams().getErrmsg());
    }

    @Test
    void submitAssessmentAsyncShouldWriteToDbAndTriggerKafkaOnFullSuccessPath() throws Exception {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        Map<String, Object> hier = hierarchy("Assessment", Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF,
                Collections.singletonList(hSection));
        hier.put(Constants.EXPECTED_DURATION, 3600);
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);
        when(assessmentUtilService.readQListfromCache(any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptyMap());
        when(assessmentUtilService.validateQumlAssessment(any(), any(), any()))
                .thenReturn(qumlResult(90.0, 10, 0, 9, 1));

        Map<String, Object> existingQuestionSet = new HashMap<>();
        existingQuestionSet.put(Constants.START_TIME, System.currentTimeMillis());
        Map<String, Object> sectionWithChildNodes = new HashMap<>();
        sectionWithChildNodes.put(Constants.CHILD_NODES, Collections.singletonList("q1"));
        existingQuestionSet.put(Constants.CHILDREN, Collections.singletonList(sectionWithChildNodes));

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.START_TIME, new Date());
        existing.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, mapper.writeValueAsString(existingQuestionSet));
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));
        when(serverProperties.getUserAssessmentSubmissionDuration()).thenReturn("10");
        when(serverProperties.getAssessmentPrimaryKeyNotAllowedCertificate())
                .thenReturn(Collections.singletonList("SomeOtherCategory"));
        when(assessmentRepository.updateUserAssesmentDataToDB(anyString(), anyString(), any(), any(), anyString(),
                any(), any(), anyString())).thenReturn(true);

        List<Map<String, Object>> submittedDetails = new ArrayList<>();
        Map<String, Object> detail = new HashMap<>();
        detail.put(Constants.NAME, "John Doe");
        submittedDetails.add(detail);
        when(assessmentRepository.fetchUserAssessmentDataFromDB(anyString(), anyString())).thenReturn(submittedDetails);

        List<Map<String, Object>> contentHierarchyDetails = new ArrayList<>();
        Map<String, Object> contentDetail = new HashMap<>();
        Map<String, Object> hierarchyObj = new HashMap<>();
        hierarchyObj.put(Constants.SOURCE, "provider1");
        hierarchyObj.put(Constants.NAME, "Course Name");
        hierarchyObj.put(Constants.POSTER_IMAGE, "http://image.url");
        contentDetail.put(Constants.HIERARCHY, mapper.writeValueAsString(hierarchyObj));
        contentHierarchyDetails.add(contentDetail);
        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any())).thenReturn(contentHierarchyDetails);

        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(
                "{}".getBytes());
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(decryptionService.decryptData(anyString())).thenReturn("decrypted-email");

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("s1")));
        SBApiResponse response = service.submitAssessmentAsync(request, false);

        assertEquals(Boolean.TRUE, response.getResult().get(Constants.PASS));
    }

    @Test
    void submitAssessmentAsyncShouldUseDefaultStructureWhenQumlResultIsEmpty() throws Exception {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        Map<String, Object> hier = hierarchy("Assessment", Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF,
                Collections.singletonList(hSection));
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);
        when(assessmentUtilService.readQListfromCache(any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptyMap());
        when(assessmentUtilService.validateQumlAssessment(any(), any(), any()))
                .thenReturn(Collections.emptyMap());

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("s1")));
        SBApiResponse response = service.submitAssessmentAsync(request, true);

        assertEquals(1, response.getResult().get(Constants.TOTAL));
        assertEquals(Boolean.FALSE, response.getResult().get(Constants.PASS));
    }

    @Test
    void submitAssessmentAsyncShouldSkipKafkaWhenDbUpdateFails() throws Exception {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        Map<String, Object> hier = hierarchy("Assessment", Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF,
                Collections.singletonList(hSection));
        hier.put(Constants.EXPECTED_DURATION, 3600);
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);
        when(assessmentUtilService.readQListfromCache(any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptyMap());
        when(assessmentUtilService.validateQumlAssessment(any(), any(), any()))
                .thenReturn(qumlResult(90.0, 10, 0, 9, 1));

        Map<String, Object> existingQuestionSet = new HashMap<>();
        existingQuestionSet.put(Constants.START_TIME, System.currentTimeMillis());
        Map<String, Object> sectionWithChildNodes = new HashMap<>();
        sectionWithChildNodes.put(Constants.CHILD_NODES, Collections.singletonList("q1"));
        existingQuestionSet.put(Constants.CHILDREN, Collections.singletonList(sectionWithChildNodes));

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.START_TIME, new Date());
        existing.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, mapper.writeValueAsString(existingQuestionSet));
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));
        when(serverProperties.getUserAssessmentSubmissionDuration()).thenReturn("10");
        when(assessmentRepository.updateUserAssesmentDataToDB(anyString(), anyString(), any(), any(), anyString(),
                any(), any(), anyString())).thenReturn(false);

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("s1")));
        SBApiResponse response = service.submitAssessmentAsync(request, false);

        assertEquals(Boolean.TRUE, response.getResult().get(Constants.PASS));
    }

    @Test
    void submitAssessmentAsyncShouldSkipWriteWhenPreviousQuestionSetHasNoStartTime() throws Exception {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        Map<String, Object> hier = hierarchy("Assessment", Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF,
                Collections.singletonList(hSection));
        hier.put(Constants.EXPECTED_DURATION, 3600);
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);
        when(assessmentUtilService.readQListfromCache(any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptyMap());
        when(assessmentUtilService.validateQumlAssessment(any(), any(), any()))
                .thenReturn(qumlResult(90.0, 10, 0, 9, 1));

        Map<String, Object> existingQuestionSet = new HashMap<>();
        Map<String, Object> sectionWithChildNodes = new HashMap<>();
        sectionWithChildNodes.put(Constants.CHILD_NODES, Collections.singletonList("q1"));
        existingQuestionSet.put(Constants.CHILDREN, Collections.singletonList(sectionWithChildNodes));

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.START_TIME, new Date());
        existing.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, mapper.writeValueAsString(existingQuestionSet));
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));
        when(serverProperties.getUserAssessmentSubmissionDuration()).thenReturn("10");

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("s1")));
        SBApiResponse response = service.submitAssessmentAsync(request, false);

        assertEquals(Boolean.TRUE, response.getResult().get(Constants.PASS));
    }

    @Test
    void submitAssessmentAsyncShouldSwallowErrorWhenDbWriteThrows() throws Exception {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        Map<String, Object> hier = hierarchy("Assessment", Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF,
                Collections.singletonList(hSection));
        hier.put(Constants.EXPECTED_DURATION, 3600);
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);
        when(assessmentUtilService.readQListfromCache(any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptyMap());
        when(assessmentUtilService.validateQumlAssessment(any(), any(), any()))
                .thenReturn(qumlResult(90.0, 10, 0, 9, 1));

        Map<String, Object> existingQuestionSet = new HashMap<>();
        existingQuestionSet.put(Constants.START_TIME, System.currentTimeMillis());
        Map<String, Object> sectionWithChildNodes = new HashMap<>();
        sectionWithChildNodes.put(Constants.CHILD_NODES, Collections.singletonList("q1"));
        existingQuestionSet.put(Constants.CHILDREN, Collections.singletonList(sectionWithChildNodes));

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.START_TIME, new Date());
        existing.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, mapper.writeValueAsString(existingQuestionSet));
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));
        when(serverProperties.getUserAssessmentSubmissionDuration()).thenReturn("10");
        when(assessmentRepository.updateUserAssesmentDataToDB(anyString(), anyString(), any(), any(), anyString(),
                any(), any(), anyString())).thenThrow(new RuntimeException("db down"));

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("s1")));
        SBApiResponse response = service.submitAssessmentAsync(request, false);

        assertEquals(Boolean.TRUE, response.getResult().get(Constants.PASS));
    }

    @Test
    void submitAssessmentAsyncShouldWriteToDbAndTriggerKafkaForSectionLevelCutOff() throws Exception {
        Map<String, Object> hSection1 = hierarchySection("s1", 50);
        Map<String, Object> hSection2 = hierarchySection("s2", 50);
        Map<String, Object> hier = hierarchy("Assessment", Constants.SECTION_LEVEL_SCORE_CUTOFF,
                Arrays.asList(hSection1, hSection2));
        hier.put(Constants.EXPECTED_DURATION, 3600);
        when(assessmentUtilService.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);
        when(assessmentUtilService.readQListfromCache(any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptyMap());
        when(assessmentUtilService.validateQumlAssessment(any(), any(), any()))
                .thenReturn(qumlResult(80.0, 10, 1, 8, 1))
                .thenReturn(qumlResult(90.0, 10, 0, 9, 1));

        Map<String, Object> existingQuestionSet = new HashMap<>();
        existingQuestionSet.put(Constants.START_TIME, System.currentTimeMillis());
        Map<String, Object> sectionWithChildNodes = new HashMap<>();
        sectionWithChildNodes.put(Constants.CHILD_NODES, Collections.singletonList("q1"));
        existingQuestionSet.put(Constants.CHILDREN, Collections.singletonList(sectionWithChildNodes));

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.START_TIME, new Date());
        existing.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, mapper.writeValueAsString(existingQuestionSet));
        when(assessmentUtilService.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));
        when(serverProperties.getUserAssessmentSubmissionDuration()).thenReturn("10");
        when(serverProperties.getAssessmentPrimaryKeyNotAllowedCertificate())
                .thenReturn(Collections.singletonList("SomeOtherCategory"));
        when(assessmentRepository.updateUserAssesmentDataToDB(anyString(), anyString(), any(), any(), anyString(),
                any(), any(), anyString())).thenReturn(true);

        List<Map<String, Object>> submittedDetails = new ArrayList<>();
        Map<String, Object> detail = new HashMap<>();
        detail.put(Constants.NAME, "John Doe");
        submittedDetails.add(detail);
        when(assessmentRepository.fetchUserAssessmentDataFromDB(anyString(), anyString())).thenReturn(submittedDetails);

        List<Map<String, Object>> contentHierarchyDetails = new ArrayList<>();
        Map<String, Object> contentDetail = new HashMap<>();
        Map<String, Object> hierarchyObj = new HashMap<>();
        hierarchyObj.put(Constants.SOURCE, "provider1");
        hierarchyObj.put(Constants.NAME, "Course Name");
        hierarchyObj.put(Constants.POSTER_IMAGE, "http://image.url");
        contentDetail.put(Constants.HIERARCHY, mapper.writeValueAsString(hierarchyObj));
        contentHierarchyDetails.add(contentDetail);
        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any())).thenReturn(contentHierarchyDetails);

        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(
                "{}".getBytes());
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(decryptionService.decryptData(anyString())).thenReturn("decrypted-email");

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Arrays.asList(submitSection("s1"), submitSection("s2")));
        SBApiResponse response = service.submitAssessmentAsync(request, false);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(85.0, response.getResult().get(Constants.OVERALL_RESULT));
        assertEquals(Boolean.TRUE, response.getResult().get(Constants.PASS));
    }
}
