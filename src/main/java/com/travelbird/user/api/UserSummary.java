package com.travelbird.user.api;

import com.travelbird.common.enums.BirdType;
import com.travelbird.user.domain.UserStatus;

public record UserSummary(
        Long userId,
        String nickname,
        BirdType birdType,
        UserStatus status
) {
}
