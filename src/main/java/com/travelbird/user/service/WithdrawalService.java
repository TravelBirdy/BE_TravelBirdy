package com.travelbird.user.service;

import com.travelbird.user.domain.User;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.global.error.ApiException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.file.repository.FileAssetRepository;
import com.travelbird.social.repository.FollowRepository;
import com.travelbird.user.repository.RefreshTokenRepository;
import com.travelbird.social.repository.UserBlockRepository;
import com.travelbird.user.repository.UserRepository;
import com.travelbird.user.repository.UserSocialAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원탈퇴 Orchestrator (공통협의 섹션7 파트간 실행순서).
 * 2단계(Part3 Post)·3단계(Part2 Trip/AI) 콘텐츠 정리는 해당 파트 계약이 이 저장소에
 * merge된 뒤 연동한다 — 아직은 Part1이 소유한 4단계 정리만 수행한다.
 */
@Service
@Transactional
public class WithdrawalService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final FollowRepository followRepository;
    private final UserBlockRepository userBlockRepository;
    private final FileAssetRepository fileAssetRepository;

    public WithdrawalService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            UserSocialAccountRepository userSocialAccountRepository,
            FollowRepository followRepository,
            UserBlockRepository userBlockRepository,
            FileAssetRepository fileAssetRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userSocialAccountRepository = userSocialAccountRepository;
        this.followRepository = followRepository;
        this.userBlockRepository = userBlockRepository;
        this.fileAssetRepository = fileAssetRepository;
    }

    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_ACTIVE));

        if (user.getStatus() == UserStatus.WITHDRAWN) {
            return;
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.USER_NOT_ACTIVE);
        }

        // 1단계 — Part1 인증 차단 준비: 활성 Refresh Token 폐기
        refreshTokenRepository.findById(userId).ifPresent(token -> token.revoke());

        // 2단계(Part3 PostWithdrawalCleanup), 3단계(Part2 TripWithdrawalCleanup)는
        // 해당 파트 구현이 merge되면 이 사이에서 호출한다.

        // 4단계 — Part1 파일/소셜/계정 정리
        fileAssetRepository.deleteAllByUserId(userId);
        followRepository.deleteAllInvolvingUser(userId);
        userBlockRepository.deleteAllInvolvingUser(userId);
        userSocialAccountRepository.deleteAllByUserId(userId);

        user.withdraw();

        // 5단계 — S3 객체 정리는 비동기 배치로 별도 처리 예정 (미구현 인프라)
    }
}
