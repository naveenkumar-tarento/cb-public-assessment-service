package com.assessment.datasecurity.impl;

import com.assessment.util.ServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EncryptionDecryptionRoundTripTest {

    @Mock
    private ServerProperties serverProperties;

    private EncryptionServiceImpl encryptionService;
    private DecryptionServiceImpl decryptionService;

    @BeforeEach
    void setUp() {
        lenient().when(serverProperties.getEncryptionKey()).thenReturn("sunbird_encryption_key");

        encryptionService = new EncryptionServiceImpl();
        encryptionService.serverProperties = serverProperties;

        decryptionService = new DecryptionServiceImpl();
        decryptionService.serverProperties = serverProperties;
    }

    @Test
    void encryptedValueShouldBeDifferentFromOriginal() {
        String original = "sensitive-value@example.com";
        String encrypted = encryptionService.encryptData(original);
        assertNotEquals(original, encrypted);
    }

    @Test
    void decryptingEncryptedValueShouldReturnOriginal() {
        String original = "sensitive-value@example.com";
        String encrypted = encryptionService.encryptData(original);
        String decrypted = decryptionService.decryptData(encrypted);

        assertEquals(original, decrypted);
    }

    @Test
    void decryptingGarbageShouldReturnInputUnchanged() {
        String garbage = "not a valid encrypted value$$$";
        String result = decryptionService.decryptData(garbage);
        assertEquals(garbage, result);
    }
}
