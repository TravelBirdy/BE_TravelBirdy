package com.travelbird.user.dto.response;

import com.travelbird.common.enums.BirdType;

public record KakaoLoginResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        boolean isNewUser,
        boolean onboardingCompleted,
        Long userId,
        String nickname,
        BirdType birdType
) {
}
