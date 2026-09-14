package com.travelbird.personality.dto.response;

import java.util.List;

public record PersonalityQuestionResponse(
        Long questionId,
        String text,
        int order,
        List<PersonalityOptionResponse> options
) {
}
