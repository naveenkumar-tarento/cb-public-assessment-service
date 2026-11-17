package com.assessment.datasecurity.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BASE64EncoderDecoderRoundTripTest {

    private final BASE64Encoder encoder = new BASE64Encoder();
    private final BASE64Decoder decoder = new BASE64Decoder();

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 57, 58, 114, 130, 200})
    void encodeThenDecodeShouldReturnOriginalBytes(int length) throws IOException {
        byte[] original = new byte[length];
        new Random(42).nextBytes(original);

        String encoded = encoder.encode(original);
        byte[] decoded = decoder.decodeBuffer(encoded);

        assertArrayEquals(original, decoded);
    }

    @Test
    void encodeBufferThenDecodeShouldReturnOriginalBytes() throws IOException {
        byte[] original = "Hello, this is a reasonably long string to span multiple base64 lines!"
                .repeat(3)
                .getBytes(StandardCharsets.UTF_8);

        String encoded = encoder.encodeBuffer(original);
        byte[] decoded = decoder.decodeBuffer(encoded);

        assertArrayEquals(original, decoded);
    }

    @Test
    void encodeAndDecodeUsingStreamsShouldRoundTrip() throws IOException {
        byte[] original = "stream based round trip".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream encodedOut = new ByteArrayOutputStream();
        encoder.encode(new ByteArrayInputStream(original), encodedOut);

        ByteArrayOutputStream decodedOut = new ByteArrayOutputStream();
        decoder.decodeBuffer(new ByteArrayInputStream(encodedOut.toByteArray()), decodedOut);

        assertArrayEquals(original, decodedOut.toByteArray());
    }

    @Test
    void encodeBufferUsingStreamsShouldRoundTrip() throws IOException {
        byte[] original = "another stream round trip with padding".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream encodedOut = new ByteArrayOutputStream();
        encoder.encodeBuffer(new ByteArrayInputStream(original), encodedOut);

        byte[] decoded = decoder.decodeBuffer(new ByteArrayInputStream(encodedOut.toByteArray()));

        assertArrayEquals(original, decoded);
    }

    @Test
    void encodeAndDecodeByteBufferShouldRoundTrip() throws IOException {
        byte[] original = "byte buffer round trip".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(original);

        String encoded = encoder.encode(buffer);
        ByteBuffer decodedBuffer = decoder.decodeBufferToByteBuffer(encoded);

        byte[] decoded = new byte[decodedBuffer.remaining()];
        decodedBuffer.get(decoded);

        assertArrayEquals(original, decoded);
    }

    @Test
    void encodeBufferByteBufferVariantShouldRoundTrip() throws IOException {
        byte[] original = "encodeBuffer byte buffer variant".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(original);

        String encoded = encoder.encodeBuffer(buffer);
        byte[] decoded = decoder.decodeBuffer(encoded);

        assertArrayEquals(original, decoded);
    }

    @Test
    void encodedOutputShouldOnlyContainBase64Alphabet() {
        byte[] original = "some arbitrary payload data 12345".getBytes(StandardCharsets.UTF_8);
        String encoded = encoder.encode(original);
        assertTrue(encoded.matches("[A-Za-z0-9+/=\\r\\n]*"));
    }

    @Test
    void emptyBufferShouldEncodeToEmptyString() {
        assertEquals("", encoder.encode(new byte[0]));
    }
}
