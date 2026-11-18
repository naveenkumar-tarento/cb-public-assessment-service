package com.assessment.util;

import com.assessment.model.SBApiResponse;
import com.assessment.util.exceptions.ProjectCommonException;
import com.assessment.util.exceptions.ResponseCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectUtilTest {

    @Test
    void isStringNullOREmptyShouldDetectNullAndEmpty() {
        assertTrue(ProjectUtil.isStringNullOREmpty(null));
        assertTrue(ProjectUtil.isStringNullOREmpty(""));
        assertTrue(ProjectUtil.isStringNullOREmpty("   "));
        assertFalse(ProjectUtil.isStringNullOREmpty("value"));
    }

    @Test
    void createServerErrorShouldBuildExceptionWithServerErrorCode() {
        ProjectCommonException exception = ProjectUtil.createServerError(ResponseCode.internalError);
        assertEquals(ResponseCode.SERVER_ERROR.getResponseCode(), exception.getResponseCode());
    }

    @Test
    void createClientExceptionShouldBuildExceptionWithClientErrorCode() {
        ProjectCommonException exception = ProjectUtil.createClientException(ResponseCode.unAuthorized);
        assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), exception.getResponseCode());
    }

    @Test
    void createDefaultResponseShouldPopulateDefaults() {
        SBApiResponse response = ProjectUtil.createDefaultResponse("api.test");
        assertEquals("api.test", response.getId());
        assertEquals(Constants.API_VERSION_1, response.getVer());
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void getDefaultHeadersShouldContainContentType() {
        Map<String, String> headers = ProjectUtil.getDefaultHeaders();
        assertEquals(Constants.APPLICATION_JSON, headers.get(Constants.CONTENT_TYPE));
    }

    @Test
    void convertSecondsToHrsAndMinutesShouldReturnEmptyForSmallValues() {
        assertEquals("", ProjectUtil.convertSecondsToHrsAndMinutes(30));
        assertEquals("", ProjectUtil.convertSecondsToHrsAndMinutes(60));
    }

    @Test
    void convertSecondsToHrsAndMinutesShouldReturnMinutesOnly() {
        assertEquals("05m", ProjectUtil.convertSecondsToHrsAndMinutes(300));
    }

    @Test
    void convertSecondsToHrsAndMinutesShouldReturnHoursOnly() {
        assertEquals("01h", ProjectUtil.convertSecondsToHrsAndMinutes(3600));
    }

    @Test
    void convertSecondsToHrsAndMinutesShouldReturnHoursAndMinutes() {
        assertEquals("01h 05m", ProjectUtil.convertSecondsToHrsAndMinutes(3900));
    }

    @Test
    void firstLetterCapitalWithSingleSpaceShouldCapitalizeEachWord() {
        assertEquals("Hello World", ProjectUtil.firstLetterCapitalWithSingleSpace("hello world"));
    }

    @Test
    void validateEmailPatternShouldAcceptValidEmail() {
        assertTrue(ProjectUtil.validateEmailPattern("user@example.com"));
    }

    @Test
    void validateEmailPatternShouldRejectInvalidEmail() {
        assertFalse(ProjectUtil.validateEmailPattern("not-an-email"));
    }

    @Test
    void validateFullNameShouldAcceptSimpleName() {
        assertTrue(ProjectUtil.validateFullName("John Doe"));
    }

    @Test
    void validateFullNameShouldRejectNameWithDigits() {
        assertFalse(ProjectUtil.validateFullName("John123"));
    }

    @Test
    void updateErrorDetailsShouldSetFailedStatusAndMessage() {
        SBApiResponse response = new SBApiResponse();
        ProjectUtil.updateErrorDetails(response, "some error", HttpStatus.BAD_REQUEST);

        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals("some error", response.getParams().getErrmsg());
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void getQuestionIdListShouldReturnEmptyListAndLogWhenRequestBodyIsMalformed() {
        Map<String, Object> questionListRequest = new HashMap<>();
        questionListRequest.put(Constants.REQUEST, "not-a-map");
        Logger logger = LoggerFactory.getLogger(ProjectUtilTest.class);

        List<String> result = ProjectUtil.getQuestionIdList(questionListRequest, logger);

        assertTrue(result.isEmpty());
    }

    @Test
    void extractQuestionSetFromAssessmentShouldReturnNullWhenStringIsBlank() throws Exception {
        Map<String, Object> existingAssessmentData = new HashMap<>();

        Map<String, Object> result = ProjectUtil.extractQuestionSetFromAssessment(existingAssessmentData,
                new ObjectMapper());

        assertNull(result);
    }

    @Test
    void extractQuestionSetFromAssessmentShouldParseJsonWhenPresent() throws Exception {
        Map<String, Object> existingAssessmentData = new HashMap<>();
        existingAssessmentData.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, "{\"key\":\"value\"}");

        Map<String, Object> result = ProjectUtil.extractQuestionSetFromAssessment(existingAssessmentData,
                new ObjectMapper());

        assertEquals("value", result.get("key"));
    }

    @Test
    void findUserSectionQuestionsShouldReturnEmptyWhenNoSectionMatches() {
        Map<String, Object> hierarchySection = new HashMap<>();
        hierarchySection.put(Constants.IDENTIFIER, "sec1");
        List<Map<String, Object>> sectionListFromSubmitRequest = new ArrayList<>();

        List<Map<String, Object>> result = ProjectUtil.findUserSectionQuestions(hierarchySection,
                sectionListFromSubmitRequest, Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF);

        assertTrue(result.isEmpty());
        assertEquals(Constants.ASSESSMENT_LEVEL_SCORE_CUTOFF, hierarchySection.get(Constants.SCORE_CUTOFF_TYPE));
    }

    @Test
    void findUserSectionQuestionsShouldReturnEmptyWhenMatchedSectionHasNoChildren() {
        Map<String, Object> hierarchySection = new HashMap<>();
        hierarchySection.put(Constants.IDENTIFIER, "sec1");
        Map<String, Object> userSection = new HashMap<>();
        userSection.put(Constants.IDENTIFIER, "sec1");
        List<Map<String, Object>> sectionListFromSubmitRequest = new ArrayList<>();
        sectionListFromSubmitRequest.add(userSection);

        List<Map<String, Object>> result = ProjectUtil.findUserSectionQuestions(hierarchySection,
                sectionListFromSubmitRequest, Constants.SECTION_LEVEL_SCORE_CUTOFF);

        assertTrue(result.isEmpty());
    }

    @Test
    void findUserSectionQuestionsShouldReturnChildrenWhenSectionMatches() {
        Map<String, Object> hierarchySection = new HashMap<>();
        hierarchySection.put(Constants.IDENTIFIER, "sec1");
        Map<String, Object> userSection = new HashMap<>();
        userSection.put(Constants.IDENTIFIER, "sec1");
        List<Map<String, Object>> children = Collections
                .singletonList(Collections.singletonMap(Constants.IDENTIFIER, "q1"));
        userSection.put(Constants.CHILDREN, children);
        List<Map<String, Object>> sectionListFromSubmitRequest = new ArrayList<>();
        sectionListFromSubmitRequest.add(userSection);

        List<Map<String, Object>> result = ProjectUtil.findUserSectionQuestions(hierarchySection,
                sectionListFromSubmitRequest, Constants.SECTION_LEVEL_SCORE_CUTOFF);

        assertEquals(children, result);
    }

    @Test
    void validateExistingAssessmentSubmissionShouldReturnNotPresentWhenNoExistingData() throws Exception {
        ServerProperties serverProperties = mock(ServerProperties.class);

        String result = ProjectUtil.validateExistingAssessmentSubmission(new ArrayList<>(), new HashMap<>(),
                new HashMap<>(), new ArrayList<>(), new ArrayList<>(), serverProperties, new ObjectMapper());

        assertEquals(Constants.USER_ASSESSMENT_DATA_NOT_PRESENT, result);
    }

    @Test
    void validateExistingAssessmentSubmissionShouldReturnStartTimeFailedWhenMissing() throws Exception {
        ServerProperties serverProperties = mock(ServerProperties.class);
        List<Map<String, Object>> existingDataList = Collections.singletonList(new HashMap<>());

        String result = ProjectUtil.validateExistingAssessmentSubmission(existingDataList, new HashMap<>(),
                new HashMap<>(), new ArrayList<>(), new ArrayList<>(), serverProperties, new ObjectMapper());

        assertEquals(Constants.READ_ASSESSMENT_START_TIME_FAILED, result);
    }

    @Test
    void validateExistingAssessmentSubmissionShouldReturnExpiredWhenSubmissionTimeTooLate() throws Exception {
        ServerProperties serverProperties = mock(ServerProperties.class);
        when(serverProperties.getUserAssessmentSubmissionDuration()).thenReturn("10");
        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.START_TIME, new Date(System.currentTimeMillis() - 10 * 60 * 1000L));
        List<Map<String, Object>> existingDataList = Collections.singletonList(existing);
        Map<String, Object> assessmentHierarchy = new HashMap<>();
        assessmentHierarchy.put(Constants.EXPECTED_DURATION, 60);

        String result = ProjectUtil.validateExistingAssessmentSubmission(existingDataList, new HashMap<>(),
                assessmentHierarchy, new ArrayList<>(), new ArrayList<>(), serverProperties, new ObjectMapper());

        assertEquals(Constants.ASSESSMENT_SUBMIT_EXPIRED, result);
    }

    @Test
    void validateExistingAssessmentSubmissionShouldReturnWrongSectionDetailsWhenSectionsMismatch() throws Exception {
        ServerProperties serverProperties = mock(ServerProperties.class);
        when(serverProperties.getUserAssessmentSubmissionDuration()).thenReturn("10");
        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.START_TIME, new Date());
        List<Map<String, Object>> existingDataList = Collections.singletonList(existing);
        Map<String, Object> assessmentHierarchy = new HashMap<>();
        assessmentHierarchy.put(Constants.EXPECTED_DURATION, 3600);
        Map<String, Object> hierarchySection = new HashMap<>();
        hierarchySection.put(Constants.IDENTIFIER, "sec1");
        Map<String, Object> wrongSection = new HashMap<>();
        wrongSection.put(Constants.IDENTIFIER, "wrongSectionId");

        String result = ProjectUtil.validateExistingAssessmentSubmission(existingDataList, new HashMap<>(),
                assessmentHierarchy, Collections.singletonList(hierarchySection),
                Collections.singletonList(wrongSection), serverProperties, new ObjectMapper());

        assertEquals(Constants.WRONG_SECTION_DETAILS, result);
    }

    @Test
    void validateExistingAssessmentSubmissionShouldReturnEmptyOnSuccessfulValidation() throws Exception {
        ServerProperties serverProperties = mock(ServerProperties.class);
        when(serverProperties.getUserAssessmentSubmissionDuration()).thenReturn("10");
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> questionSetFromAssessment = new HashMap<>();
        Map<String, Object> hierarchyChild = new HashMap<>();
        hierarchyChild.put(Constants.CHILD_NODES, Collections.singletonList("q1"));
        questionSetFromAssessment.put(Constants.CHILDREN, Collections.singletonList(hierarchyChild));

        Map<String, Object> existing = new HashMap<>();
        existing.put(Constants.START_TIME, new Date());
        existing.put(Constants.ASSESSMENT_READ_RESPONSE_KEY, mapper.writeValueAsString(questionSetFromAssessment));
        List<Map<String, Object>> existingDataList = Collections.singletonList(existing);
        Map<String, Object> assessmentHierarchy = new HashMap<>();
        assessmentHierarchy.put(Constants.EXPECTED_DURATION, 3600);
        Map<String, Object> hierarchySection = new HashMap<>();
        hierarchySection.put(Constants.IDENTIFIER, "sec1");
        Map<String, Object> matchingSection = new HashMap<>();
        Map<String, Object> question = new HashMap<>();
        question.put(Constants.IDENTIFIER, "q1");
        matchingSection.put(Constants.IDENTIFIER, "sec1");
        matchingSection.put(Constants.CHILDREN, Collections.singletonList(question));

        String result = ProjectUtil.validateExistingAssessmentSubmission(existingDataList, new HashMap<>(),
                assessmentHierarchy, Collections.singletonList(hierarchySection),
                Collections.singletonList(matchingSection), serverProperties, mapper);

        assertEquals("", result);
    }

    @Test
    void calculateAssessmentFinalResultsShouldIncludeNameWhenFlagTrue() {
        Map<String, Object> assessmentLevelResult = new HashMap<>();
        assessmentLevelResult.put(Constants.RESULT, 80.0);
        assessmentLevelResult.put(Constants.PASS_PERCENTAGE, 60);
        assessmentLevelResult.put(Constants.NAME, "Section A");
        Logger logger = LoggerFactory.getLogger(ProjectUtilTest.class);

        Map<String, Object> result = ProjectUtil.calculateAssessmentFinalResults(assessmentLevelResult, true, logger);

        assertEquals("Section A", result.get(Constants.NAME));
        assertEquals(Boolean.TRUE, result.get(Constants.PASS));
    }

    @Test
    void calculateAssessmentFinalResultsShouldExcludeNameWhenFlagFalse() {
        Map<String, Object> assessmentLevelResult = new HashMap<>();
        assessmentLevelResult.put(Constants.RESULT, 40.0);
        assessmentLevelResult.put(Constants.PASS_PERCENTAGE, 60);
        Logger logger = LoggerFactory.getLogger(ProjectUtilTest.class);

        Map<String, Object> result = ProjectUtil.calculateAssessmentFinalResults(assessmentLevelResult, false, logger);

        assertFalse(result.containsKey(Constants.NAME));
        assertEquals(Boolean.FALSE, result.get(Constants.PASS));
    }

    @Test
    void calculateAssessmentFinalResultsShouldHandleExceptionGracefully() {
        Map<String, Object> assessmentLevelResult = new HashMap<>();
        Logger logger = LoggerFactory.getLogger(ProjectUtilTest.class);

        Map<String, Object> result = ProjectUtil.calculateAssessmentFinalResults(assessmentLevelResult, false,
                logger);

        assertFalse(result.containsKey(Constants.PASS));
    }
}
