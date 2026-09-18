package com.travelbird.ai.dto.internal;
import java.time.OffsetDateTime;
public record AiServerAcceptedResponse(Long jobId, String status, OffsetDateTime acceptedAt) {}
