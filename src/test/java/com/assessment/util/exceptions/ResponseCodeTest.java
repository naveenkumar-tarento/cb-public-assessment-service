package com.assessment.util.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResponseCodeTest {

    @Test
    void getResponseShouldReturnNullForBlankErrorCode() {
        assertNull(ResponseCode.getResponse(""));
        assertNull(ResponseCode.getResponse(null));
    }

    @Test
    void getResponseShouldReturnUnauthorizedForUnauthorizedCode() {
        assertEquals(ResponseCode.unAuthorized, ResponseCode.getResponse("unauthorized"));
    }

    @Test
    void getResponseShouldMatchByErrorCode() {
        assertEquals(ResponseCode.unAuthorized,
                ResponseCode.getResponse(ResponseMessage.Key.UNAUTHORIZED_USER));
        assertEquals(ResponseCode.internalError,
                ResponseCode.getResponse(ResponseMessage.Key.INTERNAL_ERROR));
    }

    @Test
    void getResponseShouldThrowWhenNoMatchDueToUnsetErrorCodeOnNumericConstants() {
        // Current behaviour: OK/CLIENT_ERROR/SERVER_ERROR never set errorCode, so comparing
        // against them via response.getErrorCode().equals(errorCode) throws NPE for any
        // non-blank, non-"unauthorized" code that doesn't match a message-based constant.
        assertThrows(NullPointerException.class, () -> ResponseCode.getResponse("NO_SUCH_CODE"));
    }

    @Test
    void shouldExposeResponseCodeValues() {
        assertEquals(200, ResponseCode.OK.getResponseCode());
        assertEquals(400, ResponseCode.CLIENT_ERROR.getResponseCode());
        assertEquals(500, ResponseCode.SERVER_ERROR.getResponseCode());
    }

    @Test
    void getMessageShouldReturnEmptyString() {
        assertEquals("", ResponseCode.OK.getMessage(1));
    }

    @Test
    void shouldAllowSettingErrorCodeAndMessage() {
        ResponseCode code = ResponseCode.OK;
        code.setErrorCode("NEW_CODE");
        code.setErrorMessage("new message");
        code.setResponseCode(201);

        assertEquals("NEW_CODE", code.getErrorCode());
        assertEquals("new message", code.getErrorMessage());
        assertEquals(201, code.getResponseCode());

        // restore to avoid leaking mutated enum state into other tests
        code.setErrorCode(null);
        code.setErrorMessage(null);
        code.setResponseCode(200);
    }
}
