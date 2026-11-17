package com.assessment.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AssessmentServiceLoggerTest {

    private final AssessmentServiceLogger logger = new AssessmentServiceLogger(AssessmentServiceLoggerTest.class.getName());

    @Test
    void basicLoggingMethodsShouldNotThrow() {
        assertDoesNotThrow(() -> {
            logger.debug("debug message");
            logger.info("info message");
            logger.warn("warn message");
            logger.trace("trace message");
            logger.performance("performance message");
        });
    }

    @Test
    void errorWithExceptionShouldNotThrow() {
        assertDoesNotThrow(() -> logger.error(new RuntimeException("boom")));
    }

    @Test
    void errorWithMessageAndExceptionShouldNotThrow() {
        assertDoesNotThrow(() -> logger.error("custom error message", new RuntimeException("boom")));
    }

    @Test
    void fatalWithExceptionShouldNotThrow() {
        assertDoesNotThrow(() -> logger.fatal(new RuntimeException("fatal boom")));
    }

    @Test
    void logLevelEnabledFlagsShouldBeAccessible() {
        assertDoesNotThrow(logger::isDebugEnabled);
        assertDoesNotThrow(logger::isInfoEnabled);
        assertDoesNotThrow(logger::isTraceEnabled);
    }
}
