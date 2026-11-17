package com.assessment.datasecurity.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogMaskServiceImplTest {

    private final LogMaskServiceImpl service = new LogMaskServiceImpl();

    @Test
    void shouldMaskEmailKeepingFirstTwoCharsAndDomain() {
        assertEquals("ab****@example.com", service.maskEmail("abcdef@example.com"));
    }

    @Test
    void shouldMaskShortLocalPartEmail() {
        assertEquals("a@example.com", service.maskEmail("a@example.com"));
    }

    @Test
    void shouldMaskPhoneKeepingFirstFiveDigits() {
        assertEquals("98765*****", service.maskPhone("9876543210"));
    }

    @Test
    void shouldMaskShortPhoneUnchanged() {
        assertEquals("1234", service.maskPhone("1234"));
    }
}
