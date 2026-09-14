package com.travelbird.user.dto.request;

public record UpdateProfileRequest(
        String nickname,
        String introduction
) {
}
