package com.travelbird.social.dto.response;

import com.travelbird.common.enums.BirdType;
import com.travelbird.common.enums.PersonalityTrait;
import java.time.LocalDateTime;

public record FollowUserItem(
        Long userId,
        String nickname,
        BirdType birdType,
        PersonalityTrait trait,
        LocalDateTime followedAt
) {
}
