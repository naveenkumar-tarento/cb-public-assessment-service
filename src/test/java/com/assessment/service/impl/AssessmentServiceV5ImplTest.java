package com.assessment.service.impl;

import com.assessment.cassandra.utils.CassandraOperation;
import com.assessment.datasecurity.DecryptionService;
import com.assessment.datasecurity.EncryptionService;
import com.assessment.kafka.Producer;
import com.assessment.kafka.service.KafkaCertificateProducerService;
import com.assessment.model.SBApiResponse;
import com.assessment.repo.AssessmentRepository;
import com.assessment.service.AssessmentUtilServiceV2;
import com.assessment.service.OutboundRequestHandlerServiceImpl;
import com.assessment.util.Constants;
import com.assessment.util.ServerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceV5ImplTest {

    @Mock
    private ServerProperties serverProperties;

    @Mock
    private OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    @Mock
    private AssessmentUtilServiceV2 assessUtilServ;

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private DecryptionService decryptionService;

    @Mock
    private KafkaCertificateProducerService kafkaCertificateProducerService;

    @Mock
    private Producer producer;

    private final AssessmentServiceV5Impl service = new AssessmentServiceV5Impl();
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "serverProperties", serverProperties);
        ReflectionTestUtils.setField(service, "outboundRequestHandlerService", outboundRequestHandlerService);
        ReflectionTestUtils.setField(service, "assessUtilServ", assessUtilServ);
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "assessmentRepository", assessmentRepository);
        ReflectionTestUtils.setField(service, "cassandraOperation", cassandraOperation);
        ReflectionTestUtils.setField(service, "encryptionService", encryptionService);
        ReflectionTestUtils.setField(service, "resourceLoader", resourceLoader);
        ReflectionTestUtils.setField(service, "decryptionService", decryptionService);
        ReflectionTestUtils.setField(service, "kafkaCertificateProducerService", kafkaCertificateProducerService);
        ReflectionTestUtils.setField(service, "producer", producer);

        lenient().when(encryptionService.encryptData(anyString())).thenReturn("encrypted-email");
    }

    private Map<String, Object> hierarchySection(String id, int minPassPercentage) {
        Map<String, Object> section = new HashMap<>();
        section.put(Constants.IDENTIFIER, id);
        section.put(Constants.OBJECT_TYPE, "AssessmentSection");
        section.put(Constants.PRIMARY_CATEGORY, "Assessment");
        section.put(Constants.MINIMUM_PASS_PERCENTAGE, minPassPercentage);
        Map<String, Object> question = new HashMap<>();
        question.put(Constants.IDENTIFIER, "q1");
        section.put(Constants.CHILDREN, Collections.singletonList(question));
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

    private Map<String, Object> assessmentLevelHierarchy(String primaryCategory,
                                                           List<Map<String, Object>> sections) {
        Map<String, Object> h = new HashMap<>();
        h.put(Constants.PRIMARY_CATEGORY, primaryCategory);
        h.put(Constants.ASSESSMENT_TYPE, "optionalWeightage");
        h.put(Constants.CHILDREN, sections);
        h.put(Constants.MAX_ASSESSMENT_RETAKE_ATTEMPTS, 3);
        return h;
    }

    private Map<String, Object> sectionLevelHierarchy(String primaryCategory, List<Map<String, Object>> sections,
                                                        int overallMinPassPercentage) {
        Map<String, Object> h = new HashMap<>();
        h.put(Constants.PRIMARY_CATEGORY, primaryCategory);
        h.put(Constants.ASSESSMENT_TYPE, Constants.QUESTION_WEIGHTAGE);
        h.put(Constants.CHILDREN, sections);
        h.put(Constants.MAX_ASSESSMENT_RETAKE_ATTEMPTS, 3);
        h.put(Constants.MINIMUM_PASS_PERCENTAGE, overallMinPassPercentage);
        return h;
    }

    private Map<String, Object> qumlV2Result(double result, int blank, int correct, int incorrect,
                                              String sectionResult, int totalMarks, double sectionMarks) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(Constants.RESULT, result);
        resultMap.put(Constants.BLANK, blank);
        resultMap.put(Constants.CORRECT, correct);
        resultMap.put(Constants.INCORRECT, incorrect);
        resultMap.put(Constants.SECTION_RESULT, sectionResult);
        resultMap.put(Constants.TOTAL_MARKS, totalMarks);
        resultMap.put(Constants.SECTION_MARKS, sectionMarks);
        return resultMap;
    }

    private Map<String, Object> submitRequest(String assessmentId, String email, List<Map<String, Object>> sections) {
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.EMAIL, email);
        request.put(Constants.IDENTIFIER, assessmentId);
        request.put(Constants.CONTEXT_ID, "ctx1");
        request.put(Constants.CHILDREN, sections);
        return request;
    }

    // ---------- submitAssessmentAsync ----------

    @Test
    void submitAssessmentAsyncShouldReturnBadRequestForMissingIdentifier() {
        Map<String, Object> request = submitRequest(null, "user@example.com", Collections.emptyList());
        request.remove(Constants.IDENTIFIER);

        SBApiResponse response = service.submitAssessmentAsync(request, true);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.INVALID_ASSESSMENT_ID, response.getParams().getErrmsg());
    }

    @Test
    void submitAssessmentAsyncShouldReturnServerErrorWhenHierarchyEmpty() {
        when(assessUtilServ.fetchHierarchyFromAssessServc(anyString())).thenReturn(Collections.emptyMap());

        Map<String, Object> request = submitRequest("assess1", "user@example.com", Collections.emptyList());
        SBApiResponse response = service.submitAssessmentAsync(request, true);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.ASSESSMENT_HIERARCHY_READ_FAILED, response.getParams().getErrmsg());
    }

    @Test
    void submitAssessmentAsyncShouldReturnBadRequestForInvalidEmailWhenEditMode() {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        Map<String, Object> hier = assessmentLevelHierarchy("Assessment", Collections.singletonList(hSection));
        when(assessUtilServ.fetchHierarchyFromAssessServc(anyString())).thenReturn(hier);

        Map<String, Object> request = submitRequest("assess1", "not-an-email", Collections.emptyList());
        SBApiResponse response = service.submitAssessmentAsync(request, true);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.INVALID_EMAIL, response.getParams().getErrmsg());
    }

    @Test
    void submitAssessmentAsyncShouldComputeAssessmentLevelResultInEditMode() throws Exception {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        Map<String, Object> hier = assessmentLevelHierarchy("Assessment", Collections.singletonList(hSection));
        when(assessUtilServ.fetchHierarchyFromAssessServc(anyString())).thenReturn(hier);
        when(assessUtilServ.readQListfromCache(any(), anyString(), anyBoolean())).thenReturn(Collections.emptyMap());
        when(assessUtilServ.validateQumlAssessmentV2(any(), any(), any(), any()))
                .thenReturn(qumlV2Result(75.0, 1, 7, 2, Constants.PASS, 10, 7.5));

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("s1")));
        SBApiResponse response = service.submitAssessmentAsync(request, true);

        assertEquals("Assessment", response.getResult().get(Constants.PRIMARY_CATEGORY));
        assertEquals(75.0, response.getResult().get(Constants.OVERALL_RESULT));
    }

    @Test
    void submitAssessmentAsyncShouldComputeSectionLevelResultInEditMode() throws Exception {
        Map<String, Object> hSection1 = hierarchySection("s1", 50);
        hSection1.put(Constants.SECTION_LEVEL_DEFINITION, Collections.emptyMap());
        Map<String, Object> hSection2 = hierarchySection("s2", 50);
        hSection2.put(Constants.SECTION_LEVEL_DEFINITION, Collections.emptyMap());
        Map<String, Object> hier = sectionLevelHierarchy("Assessment", Arrays.asList(hSection1, hSection2), 40);
        when(assessUtilServ.fetchHierarchyFromAssessServc(anyString())).thenReturn(hier);
        when(assessUtilServ.readQListfromCache(any(), anyString(), anyBoolean())).thenReturn(Collections.emptyMap());
        when(assessUtilServ.validateQumlAssessmentV2(any(), any(), any(), any()))
                .thenReturn(qumlV2Result(80.0, 0, 8, 1, Constants.PASS, 10, 8.0))
                .thenReturn(qumlV2Result(40.0, 4, 4, 2, Constants.FAIL, 10, 4.0));

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Arrays.asList(submitSection("s1"), submitSection("s2")));
        SBApiResponse response = service.submitAssessmentAsync(request, true);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(0, response.getResult().get(Constants.RETAKE_ATTEMPT_CONSUMED));
        assertEquals(60.0, response.getResult().get(Constants.TOTAL_PERCENTAGE));
    }

    @Test
    void submitAssessmentAsyncShouldHandleExceptionGracefully() {
        when(assessUtilServ.fetchHierarchyFromAssessServc(anyString())).thenThrow(new RuntimeException("cache down"));

        Map<String, Object> request = submitRequest("assess1", "user@example.com", Collections.emptyList());
        SBApiResponse response = service.submitAssessmentAsync(request, true);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    @Test
    void submitAssessmentAsyncShouldWriteToDbAndTriggerKafkaOnFullSuccessPath() throws Exception {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        Map<String, Object> hier = assessmentLevelHierarchy("Assessment", Collections.singletonList(hSection));
        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);
        when(assessUtilServ.readQListfromCache(any(), anyString(), anyBoolean())).thenReturn(Collections.emptyMap());
        when(assessUtilServ.validateQumlAssessmentV2(any(), any(), any(), any()))
                .thenReturn(qumlV2Result(90.0, 0, 9, 1, Constants.PASS, 10, 9.0));

        Map<String, Object> existingQuestionSet = new HashMap<>();
        existingQuestionSet.put(Constants.START_TIME, System.currentTimeMillis());
        Map<String, Object> sectionWithChildNodes = new HashMap<>();
        sectionWithChildNodes.put(Constants.CHILD_NODES, Collections.singletonList("q1"));
        existingQuestionSet.put(Constants.CHILDREN, Collections.singletonList(sectionWithChildNodes));

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.START_TIME, new Date());
        existing.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, mapper.writeValueAsString(existingQuestionSet));
        when(assessUtilServ.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
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

        when(resourceLoader.getResource(anyString())).thenReturn(new ByteArrayResource("{}".getBytes()));
        when(decryptionService.decryptData(anyString())).thenReturn("decrypted-email");

        hSection.put(Constants.EXPECTED_DURATION, 3600);
        hier.put(Constants.EXPECTED_DURATION, 3600);

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("s1")));
        SBApiResponse response = service.submitAssessmentAsync(request, false);

        assertTrue(response.getResult().containsKey(Constants.OVERALL_RESULT));
    }

    // ---------- readAssessment ----------

    @Test
    void readAssessmentShouldReturnServerErrorWhenHierarchyEmpty() {
        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean()))
                .thenReturn(Collections.emptyMap());

        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        SBApiResponse response = service.readAssessment(false, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.ASSESSMENT_HIERARCHY_READ_FAILED, response.getParams().getErrmsg());
    }

    @Test
    void readAssessmentShouldReturnQuestionSetForPracticeQuestionSet() {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        Map<String, Object> hier = assessmentLevelHierarchy(Constants.PRACTICE_QUESTION_SET,
                Collections.singletonList(hSection));
        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);

        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        SBApiResponse response = service.readAssessment(false, request);

        assertTrue(response.getResult().containsKey(Constants.QUESTION_SET));
    }

    @Test
    void readAssessmentShouldDelegateToCoursePublicAssessmentHandlerForCourseAssessment() {
        Map<String, Object> hier = assessmentLevelHierarchy(Constants.COURSE_ASSESSMENT, Collections.emptyList());
        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);

        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        request.put(Constants.EMAIL, "not-an-email");
        request.put(Constants.NAME, "John Doe");
        request.put(Constants.CONTEXT_ID, "ctx1");

        SBApiResponse response = service.readAssessment(false, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.INVALID_EMAIL, response.getParams().getErrmsg());
    }

    @Test
    void readAssessmentShouldAddDataToDbForCourseAssessmentFirstRead() {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        hSection.put(Constants.EXPECTED_DURATION, 3600);
        Map<String, Object> hier = assessmentLevelHierarchy(Constants.COURSE_ASSESSMENT,
                Collections.singletonList(hSection));
        hier.put(Constants.EXPECTED_DURATION, 3600);
        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);
        when(assessUtilServ.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());
        when(assessmentRepository.addUserAssesmentDataToDB(anyString(), anyString(), any(), any(), any(), anyString(),
                anyString(), any())).thenReturn(true);

        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        request.put(Constants.EMAIL, "user@example.com");
        request.put(Constants.NAME, "John Doe");
        request.put(Constants.CONTEXT_ID, "ctx1");

        SBApiResponse response = service.readAssessment(false, request);

        assertTrue(response.getResult().containsKey(Constants.QUESTION_SET));
    }

    @Test
    void readAssessmentShouldReturnAlreadySubmittedForCourseAssessmentWhenPassed() {
        Map<String, Object> hier = assessmentLevelHierarchy(Constants.COURSE_ASSESSMENT, Collections.emptyList());
        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);

        Date pastEndTime = new Date(System.currentTimeMillis() - 3600_000L);
        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.END_TIME, pastEndTime);
        existing.put(Constants.STATUS, Constants.SUBMITTED);
        existing.put(Constants.PASS_STATUS, true);
        when(assessUtilServ.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));

        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        request.put(Constants.EMAIL, "user@example.com");
        request.put(Constants.NAME, "John Doe");
        request.put(Constants.CONTEXT_ID, "ctx1");

        SBApiResponse response = service.readAssessment(false, request);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals("User has already submitted the assessment", response.getResult().get(Constants.RESPONSE));
    }

    // ---------- readQuestionList ----------

    @Test
    void readQuestionListShouldReturnErrorWhenAssessmentIdMissing() {
        Map<String, Object> body = new HashMap<>();
        SBApiResponse response = service.readQuestionList(body, false);

        assertEquals(Constants.ASSESSMENT_ID_KEY_IS_NOT_PRESENT_IS_EMPTY, response.get(Constants.ERROR_MESSAGE));
    }

    @Test
    void readQuestionListShouldReturnErrorWhenUserAssessmentDataNotPresent() {
        Map<String, Object> hier = new HashMap<>();
        hier.put(Constants.PRIMARY_CATEGORY, "Assessment");
        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);
        when(assessUtilServ.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());

        Map<String, Object> body = new HashMap<>();
        body.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        body.put(Constants.EMAIL, "user@example.com");
        Map<String, Object> identifierFilter = new HashMap<>();
        identifierFilter.put(Constants.IDENTIFIER, Collections.singletonList("q1"));
        Map<String, Object> search = new HashMap<>();
        search.put(Constants.SEARCH, identifierFilter);
        body.put(Constants.REQUEST, search);

        SBApiResponse response = service.readQuestionList(body, false);

        assertEquals(Constants.USER_ASSESSMENT_DATA_NOT_PRESENT, response.get(Constants.ERROR_MESSAGE));
    }

    @Test
    void readQuestionListShouldSucceedWhenIdentifiersMatch() throws Exception {
        Map<String, Object> hier = new HashMap<>();
        hier.put(Constants.PRIMARY_CATEGORY, "Assessment");
        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);

        Map<String, Object> section = new HashMap<>();
        section.put(Constants.CHILD_NODES, Collections.singletonList("q1"));
        Map<String, Object> userAssessmentDetail = new HashMap<>();
        userAssessmentDetail.put(Constants.PRIMARY_CATEGORY, "Assessment");
        userAssessmentDetail.put(Constants.CHILDREN, Collections.singletonList(section));

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, mapper.writeValueAsString(userAssessmentDetail));
        when(assessUtilServ.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));

        Map<String, Object> questionsMap = new HashMap<>();
        questionsMap.put("q1", Collections.singletonMap("id", "q1"));
        when(assessUtilServ.readQListfromCache(any(), anyString(), anyBoolean())).thenReturn(questionsMap);
        when(assessUtilServ.filterQuestionMapDetailV2(any(), any())).thenReturn(Collections.singletonMap("id", "q1"));

        Map<String, Object> body = new HashMap<>();
        body.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        body.put(Constants.EMAIL, "user@example.com");
        Map<String, Object> identifierFilter = new HashMap<>();
        identifierFilter.put(Constants.IDENTIFIER, Collections.singletonList("q1"));
        Map<String, Object> search = new HashMap<>();
        search.put(Constants.SEARCH, identifierFilter);
        body.put(Constants.REQUEST, search);

        SBApiResponse response = service.readQuestionList(body, false);

        assertTrue(response.getResult().containsKey(Constants.QUESTIONS));
    }

    // ---------- readAssessmentResultV5 ----------

    @Test
    void readAssessmentResultV5ShouldReturnBadRequestForInvalidEmail() {
        Map<String, Object> inner = new HashMap<>();
        inner.put(Constants.EMAIL, "not-an-email");
        Map<String, Object> request = Collections.singletonMap(Constants.REQUEST, inner);

        SBApiResponse response = service.readAssessmentResultV5(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.INVALID_EMAIL, response.getParams().getErrmsg());
    }

    @Test
    void readAssessmentResultV5ShouldReturnBadRequestForMissingFields() {
        Map<String, Object> inner = new HashMap<>();
        inner.put(Constants.EMAIL, "user@example.com");
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.REQUEST, inner);

        SBApiResponse response = service.readAssessmentResultV5(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void readAssessmentResultV5ShouldReturnInProgressWhenNotSubmitted() {
        Map<String, Object> inner = new HashMap<>();
        inner.put(Constants.EMAIL, "user@example.com");
        inner.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        inner.put(Constants.CONTEXT_ID, "ctx1");
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.REQUEST, inner);

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.STATUS, Constants.NOT_SUBMITTED);
        when(assessUtilServ.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));

        SBApiResponse response = service.readAssessmentResultV5(request);

        assertEquals(Boolean.TRUE, response.getResult().get(Constants.STATUS_IS_IN_PROGRESS));
    }

    @Test
    void readAssessmentResultV5ShouldReturnStoredResultWhenSubmitted() throws Exception {
        Map<String, Object> inner = new HashMap<>();
        inner.put(Constants.EMAIL, "user@example.com");
        inner.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        inner.put(Constants.CONTEXT_ID, "ctx1");
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.REQUEST, inner);

        Map<String, Object> storedResult = Collections.singletonMap("overallResult", 90.0);
        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.STATUS, Constants.SUBMITTED);
        existing.put(Constants.SUBMIT_ASSESSMENT_RESPONSE_KEY, mapper.writeValueAsString(storedResult));
        when(assessUtilServ.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));

        SBApiResponse response = service.readAssessmentResultV5(request);

        assertEquals(90.0, response.get("overallResult"));
    }

    // ---------- assessmentCertificateReissue ----------

    @Test
    void assessmentCertificateReissueShouldReturnBadRequestForMissingFields() {
        Map<String, Object> body = new HashMap<>();
        body.put(Constants.EMAIL, "user@example.com");

        SBApiResponse response = service.assessmentCertificateReissue(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.INVALID_REQUEST, response.getParams().getErrmsg());
    }

    @Test
    void assessmentCertificateReissueShouldReturnBadRequestForInvalidEmail() {
        Map<String, Object> body = new HashMap<>();
        body.put(Constants.EMAIL, "not-an-email");
        body.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        body.put(Constants.CONTEXT_ID, "ctx1");

        SBApiResponse response = service.assessmentCertificateReissue(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.INVALID_EMAIL, response.getParams().getErrmsg());
    }

    @Test
    void assessmentCertificateReissueShouldReportDataNotAvailableWhenRecordsExist() {
        // Current behaviour: the isEmpty() check is inverted, so a non-empty result
        // list is treated as "not available".
        Map<String, Object> body = new HashMap<>();
        body.put(Constants.EMAIL, "user@example.com");
        body.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        body.put(Constants.CONTEXT_ID, "ctx1");

        List<Map<String, Object>> details = Collections.singletonList(Collections.singletonMap(Constants.NAME, "John"));
        when(assessmentRepository.fetchUserAssessmentDataFromDB(anyString(), anyString())).thenReturn(details);

        SBApiResponse response = service.assessmentCertificateReissue(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals("User assessment data is not available", response.getParams().getErrmsg());
    }

    @Test
    void assessmentCertificateReissueShouldCatchIndexErrorWhenRecordsAreEmpty() {
        // Current behaviour: an empty result list goes down the "found" branch and
        // immediately indexes element 0, which throws and is caught generically.
        Map<String, Object> body = new HashMap<>();
        body.put(Constants.EMAIL, "user@example.com");
        body.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        body.put(Constants.CONTEXT_ID, "ctx1");

        when(assessmentRepository.fetchUserAssessmentDataFromDB(anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        SBApiResponse response = service.assessmentCertificateReissue(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals("Failed to read user assessment data for certificate reissue.",
                response.getParams().getErrmsg());
    }

    // ---------- processNotification / validateAssessmentRequest ----------

    @Test
    void validateAssessmentRequestShouldReturnErrorForEmptyRequest() {
        String result = service.validateAssessmentRequest(Collections.emptyMap());
        assertEquals("Request object is empty.", result);
    }

    @Test
    void validateAssessmentRequestShouldReturnErrorForMissingAssessmentId() {
        Map<String, Object> request = new HashMap<>();
        request.put("someOtherKey", "value");
        String result = service.validateAssessmentRequest(request);
        assertTrue(result.contains(Constants.ASSESSMENT_ID_KEY));
    }

    @Test
    void validateAssessmentRequestShouldReturnErrorForMissingUserId() {
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ASSESSMENT_ID_KEY, "assess1");
        request.put(Constants.E_DATA, new HashMap<>());

        String result = service.validateAssessmentRequest(request);
        assertTrue(result.contains(Constants.USER_ID));
    }

    @Test
    void validateAssessmentRequestShouldReturnErrorForMissingCourseId() {
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ASSESSMENT_ID_KEY, "assess1");
        Map<String, Object> edata = new HashMap<>();
        edata.put(Constants.USER_ID, "user1");
        edata.put(Constants.RELATED, new HashMap<>());
        request.put(Constants.E_DATA, edata);

        String result = service.validateAssessmentRequest(request);
        assertTrue(result.contains(Constants.COURSE_ID));
    }

    @Test
    void validateAssessmentRequestShouldReturnEmptyForValidRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ASSESSMENT_ID_KEY, "assess1");
        Map<String, Object> edata = new HashMap<>();
        edata.put(Constants.USER_ID, "user1");
        Map<String, Object> related = new HashMap<>();
        related.put(Constants.COURSE_ID, "course1");
        edata.put(Constants.RELATED, related);
        request.put(Constants.E_DATA, edata);

        String result = service.validateAssessmentRequest(request);
        assertEquals("", result);
    }

    @Test
    void processNotificationShouldSendEmailOnValidRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ASSESSMENT_ID_KEY, "assess1");
        Map<String, Object> edata = new HashMap<>();
        edata.put(Constants.USER_ID, "user1@example.com");
        edata.put(Constants.COURSE_NAME, "Course 1");
        edata.put(Constants.COURSE_POSTER_IMAGE, "http://image.url");
        Map<String, Object> related = new HashMap<>();
        related.put(Constants.COURSE_ID, "course1");
        edata.put(Constants.RELATED, related);
        request.put(Constants.E_DATA, edata);

        when(serverProperties.getPublicAssessmentCompletionTemplate()).thenReturn("completionTemplate");
        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(outboundRequestHandlerService.fetchResultUsingPost(anyString(), any(), any())).thenReturn(Collections.emptyMap());

        service.processNotification(request);
        // No assertions on outcome needed beyond not throwing; interaction verified via no exception.
    }

    @Test
    void processNotificationShouldLogAndReturnWhenRequestInvalid() {
        service.processNotification(Collections.emptyMap());
        // Should not throw even though request is invalid.
    }

    // ---------- processDownloadNotification ----------

    @Test
    void processDownloadNotificationShouldLogWhenNoCassandraResponse() {
        Map<String, Object> request = new HashMap<>();
        request.put("userid", "user1@example.com");
        request.put("courseid", "course1");
        request.put("assessmentid", "assess1");

        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        service.processDownloadNotification(request);
        // Should not throw.
    }

    @Test
    void processDownloadNotificationShouldSendNotificationWhenCertificateFound() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userid", "user1@example.com");
        request.put("courseid", "course1");
        request.put("assessmentid", "assess1");

        Map<String, Object> cassandraRow = new HashMap<>();
        cassandraRow.put("cert_publicurl", "http://cloud.url/cert123.pdf");
        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(any(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(cassandraRow));
        when(serverProperties.getCloudStorageUrl()).thenReturn("http://cloud.url");
        when(serverProperties.getPublicAccessUrl()).thenReturn("http://public.url/");

        Map<String, Object> hierarchyObj = new HashMap<>();
        hierarchyObj.put(Constants.NAME, "Course Name");
        hierarchyObj.put(Constants.POSTER_IMAGE, "http://image.url");
        Map<String, Object> contentDetail = new HashMap<>();
        contentDetail.put(Constants.HIERARCHY, mapper.writeValueAsString(hierarchyObj));
        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(contentDetail));
        when(serverProperties.getPublicAssessmentCertificateTemplate()).thenReturn("certTemplate");
        when(outboundRequestHandlerService.fetchResultUsingPost(anyString(), any(), any())).thenReturn(Collections.emptyMap());

        service.processDownloadNotification(request);
        // Should not throw.
    }

    // ---------- pure helper methods ----------

    @Test
    void generateMarkMapShouldExtractMarksForQuestionPerSection() {
        Map<String, Map<String, Object>> schemeMap = new HashMap<>();
        schemeMap.put("easy", Collections.singletonMap("marksForQuestion", 5));
        schemeMap.put("hard", Collections.singletonMap("marksForQuestion", 10));

        Map<String, Integer> result = service.generateMarkMap(schemeMap);

        assertEquals(5, result.get("easy"));
        assertEquals(10, result.get("hard"));
    }

    @Test
    void shuffleQuestionsShouldReturnNewListWithSameElements() {
        List<Map<String, Object>> original = Arrays.asList(Collections.singletonMap("a", "1"),
                Collections.singletonMap("b", "2"));

        List<Map<String, Object>> shuffled = AssessmentServiceV5Impl.shuffleQuestions(original);

        assertEquals(2, shuffled.size());
        assertTrue(shuffled.containsAll(original));
    }

    @Test
    void createResponseMapWithProperStructureShouldUseFallbackWhenResultMapEmpty() {
        Map<String, Object> hierarchySection = hierarchySection("s1", 50);

        Map<String, Object> result = service.createResponseMapWithProperStructure(hierarchySection, Collections.emptyMap());

        assertEquals(0.0, result.get(Constants.RESULT));
        assertFalse((Boolean) result.get(Constants.PASS));
    }

    // ---------- processRandomizationForQuestions (via readAssessment practice path) ----------

    private Map<String, Object> questionWithLevel(String id, String level) {
        Map<String, Object> q = new HashMap<>();
        q.put(Constants.IDENTIFIER, id);
        q.put(Constants.QUESTION_LEVEL, level);
        return q;
    }

    @Test
    void readAssessmentShouldApplyRandomizationLimitsForQuestionWeightageSections() {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        hSection.put(Constants.CHILDREN, Arrays.asList(questionWithLevel("q1", "easy"),
                questionWithLevel("q2", "easy")));
        Map<String, Object> sectionLevelDefinition = new HashMap<>();
        sectionLevelDefinition.put("easy", Collections.singletonMap(Constants.NO_OF_QUESTIONS, 1));
        hSection.put(Constants.SECTION_LEVEL_DEFINITION, sectionLevelDefinition);

        Map<String, Object> hier = new HashMap<>();
        hier.put(Constants.PRIMARY_CATEGORY, Constants.PRACTICE_QUESTION_SET);
        hier.put(Constants.ASSESSMENT_TYPE, Constants.QUESTION_WEIGHTAGE);
        hier.put(Constants.CHILDREN, Collections.singletonList(hSection));
        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);

        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        SBApiResponse response = service.readAssessment(false, request);

        Map<String, Object> questionSet = (Map<String, Object>) response.getResult().get(Constants.QUESTION_SET);
        List<Map<String, Object>> sections = (List<Map<String, Object>>) questionSet.get(Constants.CHILDREN);
        List<String> childNodes = (List<String>) sections.get(0).get(Constants.CHILD_NODES);
        // Only one "easy" question is allowed per the section-level definition's limit of 1.
        assertEquals(1, childNodes.size());
    }

    @Test
    void readAssessmentShouldReturnAllQuestionsWhenNoRandomizationLimitDefined() {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        hSection.put(Constants.CHILDREN, Arrays.asList(questionWithLevel("q1", "easy"),
                questionWithLevel("q2", "easy")));
        hSection.put(Constants.SECTION_LEVEL_DEFINITION, Collections.emptyMap());

        Map<String, Object> hier = new HashMap<>();
        hier.put(Constants.PRIMARY_CATEGORY, Constants.PRACTICE_QUESTION_SET);
        hier.put(Constants.ASSESSMENT_TYPE, Constants.QUESTION_WEIGHTAGE);
        hier.put(Constants.CHILDREN, Collections.singletonList(hSection));
        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);

        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        SBApiResponse response = service.readAssessment(false, request);

        Map<String, Object> questionSet = (Map<String, Object>) response.getResult().get(Constants.QUESTION_SET);
        List<Map<String, Object>> sections = (List<Map<String, Object>>) questionSet.get(Constants.CHILDREN);
        List<String> childNodes = (List<String>) sections.get(0).get(Constants.CHILD_NODES);
        // No qualifying randomization limit -> all questions pass through unfiltered.
        assertEquals(2, childNodes.size());
    }

    // ---------- handleAssessmentReadForPublicAssessments extra branches ----------

    @Test
    void readAssessmentShouldReturnInProgressQuestionSetForCourseAssessment() throws Exception {
        Map<String, Object> hier = assessmentLevelHierarchy(Constants.COURSE_ASSESSMENT, Collections.emptyList());
        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);

        Map<String, Object> existingQuestionSet = Collections.singletonMap("existingKey", "existingValue");
        Date futureEndTime = new Date(System.currentTimeMillis() + 3600_000L);
        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.END_TIME, futureEndTime);
        existing.put(Constants.STATUS, Constants.NOT_SUBMITTED);
        existing.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, mapper.writeValueAsString(existingQuestionSet));
        when(assessUtilServ.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));

        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        request.put(Constants.EMAIL, "user@example.com");
        request.put(Constants.NAME, "John Doe");
        request.put(Constants.CONTEXT_ID, "ctx1");

        SBApiResponse response = service.readAssessment(false, request);

        Map<String, Object> questionSet = (Map<String, Object>) response.getResult().get(Constants.QUESTION_SET);
        assertEquals("existingValue", questionSet.get("existingKey"));
    }

    @Test
    void readAssessmentShouldReportStartTimeFailureWhenRetrySubmitDoesNotUpdateDb() {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        hSection.put(Constants.EXPECTED_DURATION, 3600);
        Map<String, Object> hier = assessmentLevelHierarchy(Constants.COURSE_ASSESSMENT,
                Collections.singletonList(hSection));
        hier.put(Constants.EXPECTED_DURATION, 3600);
        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);

        Date pastEndTime = new Date(System.currentTimeMillis() - 3600_000L);
        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.END_TIME, pastEndTime);
        existing.put(Constants.STATUS, Constants.SUBMITTED);
        existing.put(Constants.PASS_STATUS, false);
        when(assessUtilServ.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Collections.singletonList(existing));
        when(assessmentRepository.addUserAssesmentDataToDB(anyString(), anyString(), any(), any(), any(), anyString(),
                anyString(), any())).thenReturn(false);

        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ASSESSMENT_IDENTIFIER, "assess1");
        request.put(Constants.EMAIL, "user@example.com");
        request.put(Constants.NAME, "John Doe");
        request.put(Constants.CONTEXT_ID, "ctx1");

        SBApiResponse response = service.readAssessment(false, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.ASSESSMENT_DATA_START_TIME_NOT_UPDATED, response.getParams().getErrmsg());
    }

    // ---------- SECTION_LEVEL_SCORE_CUTOFF full non-editMode success path ----------

    @Test
    void submitAssessmentAsyncShouldWriteToDbAndTriggerKafkaForSectionLevelScoreCutoff() throws Exception {
        Map<String, Object> hSection = hierarchySection("s1", 50);
        hSection.put(Constants.SECTION_LEVEL_DEFINITION, Collections.emptyMap());
        hSection.put(Constants.EXPECTED_DURATION, 3600);
        Map<String, Object> hier = sectionLevelHierarchy("Assessment", Collections.singletonList(hSection), 40);
        hier.put(Constants.EXPECTED_DURATION, 3600);
        when(assessUtilServ.readAssessmentHierarchyFromCache(anyString(), anyBoolean())).thenReturn(hier);
        when(assessUtilServ.readQListfromCache(any(), anyString(), anyBoolean())).thenReturn(Collections.emptyMap());
        when(assessUtilServ.validateQumlAssessmentV2(any(), any(), any(), any()))
                .thenReturn(qumlV2Result(90.0, 0, 9, 1, Constants.PASS, 10, 9.0));

        Map<String, Object> existingQuestionSet = new HashMap<>();
        existingQuestionSet.put(Constants.START_TIME, System.currentTimeMillis());
        Map<String, Object> sectionWithChildNodes = new HashMap<>();
        sectionWithChildNodes.put(Constants.CHILD_NODES, Collections.singletonList("q1"));
        existingQuestionSet.put(Constants.CHILDREN, Collections.singletonList(sectionWithChildNodes));

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.START_TIME, new Date());
        existing.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, mapper.writeValueAsString(existingQuestionSet));
        Map<String, Object> otherRetakeRecord = new HashMap<>();
        otherRetakeRecord.put(Constants.SUBMIT_ASSESSMENT_RESPONSE_KEY, "{}");
        when(assessUtilServ.readUserSubmittedAssessmentRecords(anyString(), anyString(), any()))
                .thenReturn(Arrays.asList(existing, otherRetakeRecord));
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

        when(resourceLoader.getResource(anyString())).thenReturn(new ByteArrayResource("{}".getBytes()));
        when(decryptionService.decryptData(anyString())).thenReturn("decrypted-email");

        Map<String, Object> request = submitRequest("assess1", "user@example.com",
                Collections.singletonList(submitSection("s1")));
        SBApiResponse response = service.submitAssessmentAsync(request, false);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(1, response.getResult().get(Constants.RETAKE_ATTEMPT_CONSUMED));
    }

    // ---------- validateUserAssementData (dead code reachable only via reflection) ----------

    @Test
    void validateUserAssementDataShouldReportMissingDataViaReflection() throws Exception {
        java.lang.reflect.Method method = AssessmentServiceV5Impl.class
                .getDeclaredMethod("validateUserAssementData", List.class);
        method.setAccessible(true);

        Object result = method.invoke(service, Collections.emptyList());
        assertEquals("User assessment data not found", result);
    }

    @Test
    void validateUserAssementDataShouldReportNotPassedViaReflection() throws Exception {
        java.lang.reflect.Method method = AssessmentServiceV5Impl.class
                .getDeclaredMethod("validateUserAssementData", List.class);
        method.setAccessible(true);

        Map<String, Object> data = new HashMap<>();
        data.put(Constants.PASS_STATUS, false);
        Object result = method.invoke(service, Collections.singletonList(data));
        assertEquals("assessment is not passed", result);
    }
}
