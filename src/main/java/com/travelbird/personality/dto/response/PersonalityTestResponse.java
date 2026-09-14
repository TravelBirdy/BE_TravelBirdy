package com.travelbird.personality.dto.response;

import java.util.List;

public record PersonalityTestResponse(
        String testVersion,
        List<PersonalityQuestionResponse> questions
) {
}
