package com.travelbird.personality.dto.request;

import com.travelbird.common.enums.PersonalityTrait;

public record PersonalityTieBreakerRequest(
        Long submissionId,
        PersonalityTrait selectedTrait
) {
}
