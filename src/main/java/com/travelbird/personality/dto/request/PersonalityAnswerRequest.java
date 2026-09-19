package com.travelbird.personality.dto.request;

public record PersonalityAnswerRequest(
        Long questionId,
        Long optionId
) {
}
