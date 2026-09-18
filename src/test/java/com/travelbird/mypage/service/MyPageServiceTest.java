package com.travelbird.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.travelbird.user.domain.User;
import com.travelbird.common.enums.BirdType;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.social.repository.FollowRepository;
import com.travelbird.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private UserContentStatisticsReader userContentStatisticsReader;

    private MyPageService myPageService;
    private User activeUser;

    @BeforeEach
    void setUp() {
        myPageService = new MyPageService(userRepository, followRepository, userContentStatisticsReader);

        activeUser = User.createFromKakao("user@kakao.com");
        ReflectionTestUtils.setField(activeUser, "userId", 1L);
        ReflectionTestUtils.setField(activeUser, "status", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(activeUser, "nickname", "은진");
        ReflectionTestUtils.setField(activeUser, "birdType", BirdType.HOBANSAE);
    }

    @Test
    void getMyPage_userNotFound_throwsUserNotActive() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> myPageService.getMyPage(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_ACTIVE);
    }

    @Test
    void getMyPage_withdrawnUser_throwsUserNotActive() {
        ReflectionTestUtils.setField(activeUser, "status", UserStatus.WITHDRAWN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> myPageService.getMyPage(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_ACTIVE);
    }

    @Test
    void getMyPage_active_combinesFollowCountsAndContentStatistics() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(followRepository.countByFollowing_UserId(1L)).thenReturn(42L);
        when(followRepository.countByFollower_UserId(1L)).thenReturn(7L);
        when(userContentStatisticsReader.getStatistics(1L)).thenReturn(new UserContentStatistics(5, 3));

        var response = myPageService.getMyPage(1L);

        assertThat(response.profile().nickname()).isEqualTo("은진");
        assertThat(response.profile().birdType()).isEqualTo(BirdType.HOBANSAE);
        assertThat(response.statistics().followerCount()).isEqualTo(42L);
        assertThat(response.statistics().followingCount()).isEqualTo(7L);
        assertThat(response.statistics().postCount()).isEqualTo(5L);
        assertThat(response.statistics().visitedRegionCount()).isEqualTo(3L);
    }
}
