package com.travelbird.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.travelbird.user.dto.request.UpdateProfileRequest;
import com.travelbird.user.domain.User;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.global.error.ApiException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    private ProfileService profileService;
    private User activeUser;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(userRepository);

        activeUser = User.createFromKakao("user@kakao.com");
        ReflectionTestUtils.setField(activeUser, "userId", 1L);
        ReflectionTestUtils.setField(activeUser, "status", UserStatus.ACTIVE);
    }

    @Test
    void getProfile_userNotFound_throwsUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(1L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void getProfile_suspendedUser_throwsUserNotActive() {
        ReflectionTestUtils.setField(activeUser, "status", UserStatus.SUSPENDED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> profileService.getProfile(1L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_ACTIVE);
    }

    @Test
    void getProfile_active_returnsProfile() {
        ReflectionTestUtils.setField(activeUser, "nickname", "은진");
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        var response = profileService.getProfile(1L);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.nickname()).isEqualTo("은진");
        assertThat(response.onboardingCompleted()).isFalse();
    }

    @Test
    void updateProfile_nicknameTooShort_throwsInvalidProfileValue() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> profileService.updateProfile(1L, new UpdateProfileRequest("a", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PROFILE_VALUE);
    }

    @Test
    void updateProfile_nicknameTooLong_throwsInvalidProfileValue() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> profileService.updateProfile(1L, new UpdateProfileRequest("12345678901", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PROFILE_VALUE);
    }

    @Test
    void updateProfile_introductionTooLong_throwsInvalidProfileValue() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        String tooLong = "a".repeat(101);

        assertThatThrownBy(() -> profileService.updateProfile(1L, new UpdateProfileRequest(null, tooLong)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PROFILE_VALUE);
    }

    @Test
    void updateProfile_validValues_updatesOnlyProvidedFields() {
        ReflectionTestUtils.setField(activeUser, "introduction", "기존 소개");
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        var response = profileService.updateProfile(1L, new UpdateProfileRequest("새닉네임", null));

        assertThat(response.nickname()).isEqualTo("새닉네임");
        assertThat(response.introduction()).isEqualTo("기존 소개");
    }

    @Test
    void updateProfile_nullRequest_leavesProfileUnchanged() {
        ReflectionTestUtils.setField(activeUser, "nickname", "기존닉네임");
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        var response = profileService.updateProfile(1L, null);

        assertThat(response.nickname()).isEqualTo("기존닉네임");
    }
}
