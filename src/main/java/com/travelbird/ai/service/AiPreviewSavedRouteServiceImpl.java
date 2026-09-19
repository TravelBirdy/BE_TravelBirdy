package com.travelbird.ai.service;

import com.travelbird.ai.api.AiPreviewSavedRouteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiPreviewSavedRouteServiceImpl implements AiPreviewSavedRouteService {
  private final AiPreviewService previews;

  public AiPreviewSavedRouteServiceImpl(AiPreviewService previews) {
    this.previews = previews;
  }

  @Override
  @Transactional
  public void save(Long userId, Long previewId) {
    previews.makePermanent(userId, previewId);
  }

  @Override
  @Transactional
  public void cancel(Long userId, Long previewId) {
    previews.makeTemporary(userId, previewId);
  }
}
