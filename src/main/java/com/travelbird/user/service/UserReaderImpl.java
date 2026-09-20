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

    @Override
    public UserSummary getUserSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return new UserSummary(user.getUserId(), user.getNickname(), user.getBirdType(), user.getStatus());
    }
}
