package com.travelbird.personality.dto.response;

import com.travelbird.common.enums.BirdType;
import com.travelbird.common.enums.PersonalityTrait;

public record PartnerBirdResponse(
        boolean onboardingCompleted,
        BirdType birdType,
        String birdName,
        PersonalityTrait trait,
        String description
) {
}
