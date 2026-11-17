package com.assessment.datasecurity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class OneWayHashingTest {

    @Test
    void shouldProduceConsistentSha256HashForSameInput() {
        String hash1 = OneWayHashing.encryptVal("password123");
        String hash2 = OneWayHashing.encryptVal("password123");
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
    }

    @Test
    void shouldProduceDifferentHashesForDifferentInput() {
        String hash1 = OneWayHashing.encryptVal("password123");
        String hash2 = OneWayHashing.encryptVal("password456");
        assertNotEquals(hash1, hash2);
    }
}
