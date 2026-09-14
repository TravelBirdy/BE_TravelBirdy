package com.travelbird.personality.dto.request;

import java.util.List;

public record PersonalityTestSubmissionRequest(
        String testVersion,
        List<PersonalityAnswerRequest> answers
) {
}
