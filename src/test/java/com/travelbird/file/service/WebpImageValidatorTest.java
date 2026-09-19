package com.travelbird.file.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class WebpImageValidatorTest {

    private final WebpImageValidator validator = new WebpImageValidator();

    @Test
    void hasWebpSignature_validRiffWebpHeader_returnsTrue() {
        byte[] bytes = webpHeader("WEBP");

        assertThat(validator.hasWebpSignature(bytes)).isTrue();
    }

    @Test
    void hasWebpSignature_tooShort_returnsFalse() {
        byte[] bytes = "RIFF".getBytes(StandardCharsets.US_ASCII);

        assertThat(validator.hasWebpSignature(bytes)).isFalse();
    }

    @Test
    void hasWebpSignature_wrongRiffMarker_returnsFalse() {
        byte[] bytes = "XIFF\0\0\0\0WEBP".getBytes(StandardCharsets.US_ASCII);

        assertThat(validator.hasWebpSignature(bytes)).isFalse();
    }

    @Test
    void hasWebpSignature_notWebpFormatMarker_returnsFalse() {
        // JPEG를 image/webp라고 속여서 올린 경우를 흉내 — RIFF는 맞지만 포맷 마커가 WEBP가 아님
        byte[] bytes = webpHeader("JPEG");

        assertThat(validator.hasWebpSignature(bytes)).isFalse();
    }

    private byte[] webpHeader(String formatMarker) {
        String header = "RIFF" + "\0\0\0\0" + formatMarker;
        return header.getBytes(StandardCharsets.US_ASCII);
    }
}
