package com.travelbird.personality.service;

import com.travelbird.personality.dto.response.PersonalityTestCompletedResponse;
import com.travelbird.personality.dto.response.PersonalityTieBreakerRequiredResponse;

public sealed interface PersonalityTestSubmissionResult {

    record Completed(PersonalityTestCompletedResponse response) implements PersonalityTestSubmissionResult {
    }

    record TieBreakerRequired(PersonalityTieBreakerRequiredResponse response) implements PersonalityTestSubmissionResult {
    }
}
