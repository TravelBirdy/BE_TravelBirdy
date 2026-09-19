package com.travelbird.mypage.dto.response;

import com.travelbird.common.enums.BirdType;

public record MyPageProfile(
        Long userId,
        String nickname,
        String introduction,
        BirdType birdType
) {
}
