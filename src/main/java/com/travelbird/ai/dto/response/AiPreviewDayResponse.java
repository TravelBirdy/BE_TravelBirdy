package com.travelbird.ai.dto.response;
import java.util.List;
public record AiPreviewDayResponse(int day, List<AiPreviewPlaceResponse> places) {}
