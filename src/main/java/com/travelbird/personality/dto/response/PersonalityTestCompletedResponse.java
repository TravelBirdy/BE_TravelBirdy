package com.travelbird.personality.dto.response;

import com.travelbird.common.enums.BirdType;
import com.travelbird.personality.domain.PersonalityResultStatus;
import com.travelbird.common.enums.PersonalityTrait;

public record PersonalityTestCompletedResponse(
        Long submissionId,
        PersonalityResultStatus resultStatus,
        boolean onboardingCompleted,
        BirdType birdType,
        String birdName,
        PersonalityTrait trait,
        String description
) {
}
