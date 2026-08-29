package com.travelbird.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CorsPropertiesTest {

    @Test
    void allowedOriginsAreCopiedAndPreserved() {
        List<String> origins = new ArrayList<>(List.of("http://localhost:3000"));
        CorsProperties properties = new CorsProperties(origins);

        origins.add("http://malicious.example");

        assertThat(properties.allowedOrigins()).containsExactly("http://localhost:3000");
        assertThat(properties.allowedOrigins()).doesNotContain("http://malicious.example");
    }
}