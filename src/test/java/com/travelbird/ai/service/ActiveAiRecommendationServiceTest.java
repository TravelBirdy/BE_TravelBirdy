package com.travelbird.ai.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.travelbird.ai.dto.request.AiRecommendationRequest;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.user.api.UserReader;
import org.junit.jupiter.api.Test;

class ActiveAiRecommendationServiceTest {

  @Test
  void inactiveUserCannotQueueOrPoll() {
    var users = mock(UserReader.class);
    var delegate = mock(AiRecommendationService.class);
    doThrow(new BusinessException(ErrorCode.USER_NOT_ACTIVE))
        .when(users).validateActiveUser(3L);
    var service = new ActiveAiRecommendationService(users, delegate);

    assertThatThrownBy(() -> service.request(3L, mock(AiRecommendationRequest.class)))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.status(3L, 7L))
        .isInstanceOf(BusinessException.class);
    verifyNoInteractions(delegate);
  }
}
