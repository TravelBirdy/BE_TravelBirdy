package com.travelbird.savedroute.service;

import com.travelbird.ai.api.AiPreviewSavedRouteService;
import com.travelbird.ai.service.AiPreviewService;
import com.travelbird.savedroute.repository.AiPreviewSavedRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiPreviewSavedRouteServiceImpl implements AiPreviewSavedRouteService {
  private final AiPreviewSavedRouteRepository routes;
  private final AiPreviewService previews;
  public AiPreviewSavedRouteServiceImpl(AiPreviewSavedRouteRepository routes, AiPreviewService previews) {
    this.routes = routes;
    this.previews = previews;
  }
  @Override @Transactional public void save(Long userId, Long previewId) {
    previews.makePermanent(userId, previewId);
    routes.savePreview(userId, previewId);
  }
  @Override @Transactional public void cancel(Long userId, Long previewId) {
    previews.get(userId, previewId);
    routes.deletePreview(userId, previewId);
    if (routes.countPreview(previewId) == 0) previews.makeTemporary(userId, previewId);
  }
}
