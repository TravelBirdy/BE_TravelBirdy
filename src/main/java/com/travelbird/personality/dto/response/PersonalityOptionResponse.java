package com.travelbird.personality.dto.response;

public record PersonalityOptionResponse(
        Long optionId,
        String text,
        int order
) {
}
