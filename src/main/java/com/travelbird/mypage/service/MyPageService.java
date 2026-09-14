package com.travelbird.mypage.service;

import com.travelbird.mypage.dto.response.MyPageProfile;
import com.travelbird.mypage.dto.response.MyPageResponse;
import com.travelbird.mypage.dto.response.MyPageStatistics;
import com.travelbird.user.domain.User;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.global.error.ApiException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.social.repository.FollowRepository;
import com.travelbird.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final UserContentStatisticsReader userContentStatisticsReader;

    public MyPageService(
            UserRepository userRepository,
            FollowRepository followRepository,
            UserContentStatisticsReader userContentStatisticsReader
    ) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.userContentStatisticsReader = userContentStatisticsReader;
    }

    public MyPageResponse getMyPage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_ACTIVE));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.USER_NOT_ACTIVE);
        }

        long followerCount = followRepository.countByFollowing_UserId(userId);
        long followingCount = followRepository.countByFollower_UserId(userId);
        UserContentStatistics contentStatistics = userContentStatisticsReader.getStatistics(userId);

        MyPageProfile profile = new MyPageProfile(
                user.getUserId(), user.getNickname(), user.getIntroduction(), user.getBirdType());
        MyPageStatistics statistics = new MyPageStatistics(
                contentStatistics.postCount(), contentStatistics.visitedRegionCount(), followerCount, followingCount);

        return new MyPageResponse(profile, statistics);
    }
}
