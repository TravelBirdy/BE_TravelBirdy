package com.travelbird.ai.dto.response;
import java.time.LocalDateTime;
public record ApplyAiPreviewResponse(Long tripId, String applyType, LocalDateTime appliedAt) {}
