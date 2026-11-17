package com.assessment.util;

import com.assessment.model.SBApiResponse;
import com.assessment.util.exceptions.ProjectCommonException;
import com.assessment.util.exceptions.ResponseCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
