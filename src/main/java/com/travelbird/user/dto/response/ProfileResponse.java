package com.travelbird.user.dto.response;

import com.travelbird.common.enums.BirdType;
import com.travelbird.user.domain.UserRole;

public record ProfileResponse(
        Long userId,
        String nickname,
        String introduction,
        boolean onboardingCompleted,
        BirdType birdType,
        UserRole role
) {
}
