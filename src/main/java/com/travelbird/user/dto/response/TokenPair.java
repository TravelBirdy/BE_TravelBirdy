package com.travelbird.user.dto.response;

public record TokenPair(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {
}
