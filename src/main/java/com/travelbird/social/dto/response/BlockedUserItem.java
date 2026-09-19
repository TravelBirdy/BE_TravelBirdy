package com.travelbird.social.dto.response;

import com.travelbird.common.enums.BirdType;

public record BlockedUserItem(
        Long userId,
        String nickname,
        BirdType birdType
) {
}
