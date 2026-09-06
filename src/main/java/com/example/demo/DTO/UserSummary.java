package com.example.demo.DTO;

import com.example.demo.enums.BirdType;
import com.example.demo.enums.UserStatus;

public record UserSummary(
        Long userId,
        String nickname,
        BirdType birdType,
        UserStatus status
) {
}
