package com.assessment.datasecurity;

import com.assessment.datasecurity.impl.LogMaskServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataMaskingServiceTest {

    private final DataMaskingService service = new LogMaskServiceImpl();

    @Test
    void isMaskedShouldDetectAsterisk() {
        assertTrue(service.isMasked("ab***"));
        assertFalse(service.isMasked("plain"));
    }

    @Test
    void maskDataShouldReturnUnchangedForShortOrBlankInput() {
        assertEquals("", service.maskData(""));
        assertEquals("abc", service.maskData("abc"));
    }

    @Test
    void maskDataShouldMaskAllButLastFourCharacters() {
        assertEquals("*2345", service.maskData("12345"));
        assertEquals("******7890", service.maskData("1234567890"));
    }

    @Test
    void maskOtpShouldMaskSixDigitOtpKeepingFirstFour() {
        assertEquals("1234**", service.maskOTP("123456"));
    }

    @Test
    void maskOtpShouldMaskShortOtpKeepingFirstTwo() {
        assertEquals("12**", service.maskOTP("1234"));
    }
}
