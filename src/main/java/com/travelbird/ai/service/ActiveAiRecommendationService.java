package com.travelbird.ai.service;

import com.travelbird.ai.dto.request.AiRecommendationRequest;
import com.travelbird.ai.dto.request.AiRouteRecommendationRequest;
import com.travelbird.ai.dto.response.AiJobAcceptedResponse;
import com.travelbird.ai.dto.response.AiJobStatusResponse;
import com.travelbird.user.api.UserReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActiveAiRecommendationService {
  private final UserReader users;
  private final AiRecommendationService delegate;

  public ActiveAiRecommendationService(UserReader users, AiRecommendationService delegate) {
    this.users = users;
    this.delegate = delegate;
  }

  @Transactional
  public AiJobAcceptedResponse request(Long userId, AiRecommendationRequest request) {
    users.validateActiveUser(userId);
    return delegate.request(userId, request);
  }

  @Transactional
  public AiJobAcceptedResponse requestTrip(Long userId, Long tripId,
      AiRouteRecommendationRequest request) {
    users.validateActiveUser(userId);
    return delegate.requestTrip(userId, tripId, request);
  }

  @Transactional(readOnly = true)
  public AiJobStatusResponse status(Long userId, Long jobId) {
    users.validateActiveUser(userId);
    return delegate.status(userId, jobId);
  }
}
