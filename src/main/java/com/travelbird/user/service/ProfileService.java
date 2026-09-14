package com.travelbird.user.service;

import com.travelbird.user.dto.response.ProfileResponse;
import com.travelbird.user.dto.request.UpdateProfileRequest;
import com.travelbird.user.domain.User;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.global.error.ApiException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileService {

    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 10;
    private static final int INTRODUCTION_MAX_LENGTH = 100;

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ProfileResponse getProfile(Long userId) {
        return toResponse(getActiveUser(userId));
    }

    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getActiveUser(userId);

        String nickname = request == null ? null : request.nickname();
        String introduction = request == null ? null : request.introduction();

        if (nickname != null && (nickname.length() < NICKNAME_MIN_LENGTH || nickname.length() > NICKNAME_MAX_LENGTH)) {
            throw new ApiException(ErrorCode.INVALID_PROFILE_VALUE);
        }
        if (introduction != null && introduction.length() > INTRODUCTION_MAX_LENGTH) {
            throw new ApiException(ErrorCode.INVALID_PROFILE_VALUE);
        }

        user.updateProfile(nickname, introduction);
        return toResponse(user);
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.USER_NOT_ACTIVE);
        }
        return user;
    }

    private ProfileResponse toResponse(User user) {
        return new ProfileResponse(
                user.getUserId(), user.getNickname(), user.getIntroduction(),
                user.isOnboardingCompleted(), user.getBirdType(), user.getRole());
    }
}
