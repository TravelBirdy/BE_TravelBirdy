package com.travelbird.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record InternalAiProperties(
    String internalKey
) {
    public InternalAiProperties {
        if (internalKey == null || internalKey.isBlank()) {
            throw new IllegalArgumentException("app.ai.internal-key must not be blank");
        }
    }
}
