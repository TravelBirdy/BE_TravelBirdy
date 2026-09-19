package com.travelbird.ai.service;
import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Component;
@Component public class AiPreviewExpiryWorker{private final AiPreviewExpiryService service;public AiPreviewExpiryWorker(AiPreviewExpiryService s){service=s;}@Scheduled(fixedDelayString="${app.ai.preview-expiry-scan-delay-ms:60000}")public void expire(){service.expireDuePreviews();}}
