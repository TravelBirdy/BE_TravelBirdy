package com.travelbird.ai.service;

import com.travelbird.ai.dto.request.AiRecommendationRequest;
import com.travelbird.ai.dto.request.AiRouteRecommendationRequest;
import com.travelbird.ai.dto.response.AiJobAcceptedResponse;
import com.travelbird.ai.dto.response.AiJobStatusResponse;
import com.travelbird.user.service.ActiveUserGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActiveAiRecommendationService {
  private final ActiveUserGuard users;
  private final AiRecommendationService delegate;

  public ActiveAiRecommendationService(ActiveUserGuard users, AiRecommendationService delegate) {
    this.users = users;
    this.delegate = delegate;
  }

  @Transactional
  public AiJobAcceptedResponse request(Long userId, AiRecommendationRequest request) {
    users.requireActiveUserForUpdate(userId);
    return delegate.request(userId, request);
  }

  @Transactional
  public AiJobAcceptedResponse requestTrip(Long userId, Long tripId,
      AiRouteRecommendationRequest request) {
    users.requireActiveUserForUpdate(userId);
    return delegate.requestTrip(userId, tripId, request);
  }

  @Transactional(readOnly = true)
  public AiJobStatusResponse status(Long userId, Long jobId) {
    users.requireActiveUser(userId);
    return delegate.status(userId, jobId);
  }
}
