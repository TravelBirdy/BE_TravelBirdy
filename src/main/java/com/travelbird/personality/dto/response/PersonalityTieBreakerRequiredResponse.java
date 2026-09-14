package com.travelbird.personality.dto.response;

import com.travelbird.common.enums.BirdType;
import com.travelbird.personality.domain.PersonalityResultStatus;
import com.travelbird.common.enums.PersonalityTrait;
import java.util.List;

public record PersonalityTieBreakerRequiredResponse(
        Long submissionId,
        PersonalityResultStatus resultStatus,
        boolean onboardingCompleted,
        BirdType birdType,
        List<PersonalityTrait> tiedTraits
) {
}
