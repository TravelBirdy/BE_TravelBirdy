package com.travelbird.ai.dto.request;
import java.util.List;
public record UpdateAiPreviewDayRequest(Integer day, List<UpdateAiPreviewPlaceRequest> places) {}
