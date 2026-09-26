package com.travelbird.user.service;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.user.api.UserReader;
import com.travelbird.user.api.UserSummary;
import com.travelbird.user.domain.User;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.user.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class UserReaderImpl implements UserReader {

    private final UserRepository userRepository;

    public UserReaderImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void validateActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_ACTIVE));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_ACTIVE);
        }
    }

    /**
     * 호출부(Community/SavedRoute/PostDetail 등)는 카드·목록을 조합하며 유저를 못 찾는
     * 경우를 흔히 catch로 삼키고 그 항목만 placeholder로 빼는데, 이 메서드가 일반
     * {@code @Transactional}이면 삼킨 예외도 바깥 트랜잭션을 rollback-only로 만들어
     * 커밋 시점에 {@code UnexpectedRollbackException}이 난다(PR#21 리뷰에서 재현) —
     * {@code noRollbackFor}로 이 메서드의 예외만 롤백 표시에서 제외한다.
     */
    @Override
    @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
    public UserSummary getUserSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return new UserSummary(user.getUserId(), user.getNickname(), user.getBirdType(), user.getStatus());
    }
}
