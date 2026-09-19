package com.travelbird.ai.dto.internal;

import java.time.OffsetDateTime;

public record AiCallbackResponse(boolean accepted, Long jobId, Long previewId,
    OffsetDateTime processedAt) {}
